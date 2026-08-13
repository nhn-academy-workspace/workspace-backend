package com.booking.backend.domain.notification.service;

import com.booking.backend.domain.book.entity.Booking;
import com.booking.backend.domain.book.entity.BookingMember;
import com.booking.backend.domain.book.repository.BookingMemberRepository;
import com.booking.backend.domain.notification.dto.NotificationResponse;
import com.booking.backend.domain.notification.entity.NotiType;
import com.booking.backend.domain.notification.entity.Notification;
import com.booking.backend.domain.notification.entity.TargetType;
import com.booking.backend.domain.notification.repository.NotificationRepository;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.entity.Role;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.exception.exception.InsufficientPermissionException;
import com.booking.backend.exception.exception.MemberNotFoundException;
import com.booking.backend.exception.exception.NotificationNotFoundException;
import com.booking.backend.exception.exception.ReminderMessageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import static com.booking.backend.domain.notification.entity.NotificationChannel.TELEGRAM;
import static com.booking.backend.domain.notification.entity.TargetType.MEMBER;

// 누구에게, 무엇을, 몇 번 보내지는지를 결정
// NotificationEventListener와 NotificationScheduler 양쪽에서 호출됨.
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final BookingMemberRepository bookingMemberRepository;
    private final NotificationSender notificationSender;
    private final ReminderSender reminderSender;

    private static final DateTimeFormatter KOREAN_DATETIME =
            DateTimeFormatter.ofPattern("MM/dd(E) HH:mm", Locale.KOREAN);

    // NotificationScheduler가 시작/종료 5분 전 대상 예약마다 호출.
    // 팀 담당 TA만 보냄
    @Transactional
    public void notifyBooking(Booking booking, NotiType notiType) {
        List<Member> participants = bookingMemberRepository.findByBookingId(booking.getId()).stream()
                .map(BookingMember::getMember)
                .toList();
        List<Member> teamTa = resolveTeamTa(booking.getTeam().getTaId());

        String message = buildBookingMessage(booking, notiType);

        Stream.concat(participants.stream(), teamTa.stream())
                .distinct() // 같은 영속성 컨텍스트 내 조회라 동일 PK는 동일 인스턴스로 반환
                .filter(Member::isNotificationEnabled)
                .forEach(member -> reminderSender.sendOne(booking, notiType, member, message));
    }

    // 팀 전담 TA 찾기
    private List<Member> resolveTeamTa(Long taId) {
        if (taId == null) {
            return List.of();
        }
        return memberRepository.findById(taId)
                .map(List::of)
                .orElse(List.of());
    }

    // 호출 대상(개인/팀), 호출을 건 TA
    @Transactional
    public void notifyCall(TargetType type, Long targetId, Long callerId, String message) {
        List<Member> targets = switch (type) {
            case MEMBER -> List.of(memberRepository.findById(targetId)
                    .orElseThrow(() -> new MemberNotFoundException("해당 멤버가 존재하지 않습니다. : " + targetId)));
            case TEAM -> memberRepository.findByTeamId(targetId);
        };
        Member caller = memberRepository.findById(callerId)
                .orElseThrow(() -> new MemberNotFoundException("호출한 TA가 존재하지 않습니다. : " + callerId));

        Stream.concat(targets.stream(), Stream.of(caller))
                .distinct()
                .filter(Member::isNotificationEnabled)
                .forEach(member -> createSingleCall(member, message));
    }


    private void createSingleCall(Member member, String message) {
        Notification notification = Notification.createPending(null, NotiType.CALL, MEMBER, member.getId(), TELEGRAM, message);
        notificationRepository.save(notification);
        notificationSender.send(notification);
    }

    private String buildBookingMessage(Booking booking, NotiType notiType) {
        String roomName = booking.getRoom().getName();
        String teamName = booking.getTeam().getName();
        String start = booking.getStartTime().format(KOREAN_DATETIME);
        String end = booking.getEndTime().format(KOREAN_DATETIME);
        return switch (notiType) {
            case START_REMINDER -> "[%s] %s팀 예약이 5분 후(%s) 시작합니다.".formatted(roomName, teamName, start);
            case END_REMINDER -> "[%s] %s팀 예약이 5분 후(%s) 종료됩니다.".formatted(roomName, teamName, end);
            case CANCELLED -> "[%s] %s팀 예약이 취소되었습니다.".formatted(roomName, teamName);
            case TIME_CHANGED -> "[%s] %s팀 예약 시간이 %s ~ %s로 변경되었습니다.".formatted(roomName, teamName, start, end);
            default -> throw new ReminderMessageException("notifyBooking은 예약 관련 NotiType만 지원합니다: " + notiType);
        };
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long memberId) {
        return notificationRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(MEMBER, memberId)
                .stream()
                .map(n -> new NotificationResponse(n.getId(), n.getNotiType(), n.getMessage(), n.getStatus(), n.getSentAt(), n.getReadAt()))
                .toList();
    }

    @Transactional
    public void markRead(Long memberId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("해당 알림을 찾을 수 없습니다. : " + notificationId));

        boolean isOwner = notification.getTargetType() == MEMBER
                && Objects.equals(notification.getTargetId(), memberId);
        if (!isOwner) {
            throw new InsufficientPermissionException("본인 대상 알림만 읽음 처리할 수 있습니다.");
        }

        notification.markRead();
    }

    @Transactional
    public void updateNotificationPreference(Member requester, Long targetMemberId, boolean enabled) {
        boolean isSelfOrTa = Objects.equals(requester.getId(), targetMemberId) || requester.getRole() == Role.TA;
        if (!isSelfOrTa) {
            throw new InsufficientPermissionException("알림 설정 수정 권한이 없습니다. (본인이나 TA만 알림 설정 수정 가능)");
        }

        Member target = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new MemberNotFoundException("해당 멤버가 존재하지 않습니다. : " + targetMemberId));
        target.updateNotificationPreference(enabled);
    }

    @Transactional(readOnly = true)
    public boolean isChatLinked(Long memberId) {
        return memberRepository.findById(memberId)
                .map(Member::hasChatLinked)
                .orElse(false);
    }
}
