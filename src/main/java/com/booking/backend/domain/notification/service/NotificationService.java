package com.booking.backend.domain.notification.service;

import com.booking.backend.domain.book.entity.Booking;
import com.booking.backend.domain.book.entity.BookingMember;
import com.booking.backend.domain.book.repository.BookingMemberRepository;
import com.booking.backend.domain.notification.dto.NotificationResponse;
import com.booking.backend.domain.notification.entity.NotiType;
import com.booking.backend.domain.notification.entity.Notification;
import com.booking.backend.domain.notification.entity.NotificationStatus;
import com.booking.backend.domain.notification.entity.TargetType;
import com.booking.backend.exception.exception.InsufficientPermissionException;
import com.booking.backend.exception.exception.ReminderMessageException;
import com.booking.backend.domain.notification.repository.NotificationRepository;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.entity.Role;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.exception.exception.MemberNotFoundException;
import com.booking.backend.exception.exception.NotificationNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.booking.backend.domain.notification.entity.NotificationChannel.TELEGRAM;
import static com.booking.backend.domain.notification.entity.TargetType.MEMBER;

// notification-design.md #4 — "누구에게, 무엇을, 한 번만" 보내지는지를 결정하는 도메인 로직의 중심.
// NotificationEventListener와 NotificationScheduler 양쪽에서 호출됨.
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final BookingMemberRepository bookingMemberRepository;
    private final NotificationSender notificationSender;

    private static final int MAX_RETRY_COUNT = 3;

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
                .distinct() // 같은 영속성 컨텍스트 내 조회라 동일 PK는 동일 인스턴스로 반환됨(Hibernate 1차 캐시)
                .filter(Member::isNotificationEnabled)
                .forEach(member -> createSingleReminder(booking, notiType, member, message));
    }

    // 팀 전담 TA 한 명만 대상으로 함(전체 TA 아님) — taId 미배정이면 빈 리스트
    private List<Member> resolveTeamTa(Long taId) {
        if (taId == null) {
            return List.of();
        }
        return memberRepository.findById(taId)
                .map(List::of)
                .orElse(List.of());
    }

    private void createSingleReminder(Booking booking, NotiType notiType, Member member, String message) {
        boolean alreadyExists = notificationRepository
                .existsByBookingAndNotiTypeAndTargetTypeAndTargetId(booking, notiType, MEMBER, member.getId());
        if (alreadyExists) {
            return;
        }

        Notification notification = Notification.createPending(booking, notiType, MEMBER, member.getId(), TELEGRAM, message);
        try {
            // UNIQUE(book_id, type, target_id) 제약 위반 시 "이미 발송(예정)"으로 간주하고 무시
            notificationRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException e) {
            log.info("이미 생성된 알림 — 무시 | bookingId={}, notiType={}, memberId={}", booking.getId(), notiType, member.getId());
            return;
        }

        notificationSender.send(notification);
    }

    private String buildReminderMessage(Booking booking, NotiType notiType) {
        String roomName = booking.getRoom().getName();
        return switch (notiType) {
            case START_REMINDER -> "[%s] 예약이 5분 후(%s) 시작합니다.".formatted(roomName, booking.getStartTime());
            case END_REMINDER -> "[%s] 예약이 5분 후(%s) 종료됩니다.".formatted(roomName, booking.getEndTime());
            case CANCELLED -> "[%s] 예약이 취소되었습니다.".formatted(roomName);
            case TIME_CHANGED -> "[%s] 예약 시간이 %s ~ %s로 변경되었습니다.".formatted(roomName, booking.getStartTime(), booking.getEndTime());
            default -> throw new ReminderMessageException("notifyBooking은 예약 관련 NotiType만 지원합니다: " + notiType);
        };
    }

    // NotificationScheduler의 재시도 스윕이 호출. spring-retry의 @Retryable이 이미 실패한 뒤,
    // 시간이 좀 지나서 다시 시도하는 더 느슨한 세이프티넷 (역할이 달라서 중복 아님).
    @Transactional
    public void retryFailed() {
        List<Notification> notificationList = notificationRepository.findByStatusIn(List.of(NotificationStatus.FAILED));

        notificationList.stream()
                .filter(notification -> notification.getRetryCount() < MAX_RETRY_COUNT)
                .forEach(notificationSender::send);
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

    // MemberNotificationController의 updateNotificationPreference가 호출.
    // 컨트롤러에 @Transactional이 없어 엔티티 변경이 더티체킹으로 반영 안 되는 문제가 있어서 이쪽으로 옮김.
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
}
