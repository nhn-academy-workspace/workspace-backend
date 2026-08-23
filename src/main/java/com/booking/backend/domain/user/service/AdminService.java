package com.booking.backend.domain.user.service;

import com.booking.backend.domain.book.dto.BookingResponse;
import com.booking.backend.domain.book.entity.Booking;
import com.booking.backend.domain.book.repository.BookingRepository;
import com.booking.backend.domain.notification.entity.NotiType;
import com.booking.backend.domain.notification.event.CallRequestEvent;
import com.booking.backend.domain.notification.event.NotificationRequestedEvent;
import com.booking.backend.domain.notification.telegram.TelegramClient;
import com.booking.backend.domain.user.dto.CallRequest;
import com.booking.backend.domain.user.dto.MemberResponse;
import com.booking.backend.domain.user.dto.ResetPasswordResponse;
import com.booking.backend.domain.user.dto.TeamResponse;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.entity.Role;
import com.booking.backend.domain.user.entity.Team;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.domain.user.repository.TeamRepository;
import com.booking.backend.exception.exception.*;
import com.booking.backend.util.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final TelegramClient telegramClient;

    @Transactional(readOnly = true)
    public List<TeamResponse> getAll() {

        List<Member> members = memberRepository.findAllStudent();
        List<Team> teams = teamRepository.findAll();

        Map<Long, Team> teamsById = teams.stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));

        Map<Long, List<Member>> membersByTeam = members.stream()
                .collect(Collectors.groupingBy(m -> m.getTeam().getId()));


        List<TeamResponse> res = new ArrayList<>();

        for(Long teamId : teamsById.keySet()) {
            List<TeamResponse.MemberInfo> memberInfos = membersByTeam
                    .getOrDefault(teamId, List.of())
                    .stream()
                    .map(TeamResponse.MemberInfo::new)
                    .toList();

            res.add(new TeamResponse(teamId, teamsById.get(teamId).getName(), memberInfos));
        }

        return res;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getAllMembers() {
        List<Member> members = memberRepository.findAllStudentWithTeam();

        return members.stream()
                .map(MemberResponse::new)
                .toList();
    }

    @Transactional
    public MemberResponse changeTeam(Long memberId, Long teamId) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new MemberNotFoundException("해당 멤버는 찾을 수 없습니다. : " + memberId)
        );

        if(member.getRole() != Role.STUDENT) {
            throw new InvalidMemberException("수강생만 팀을 재배정할 수 있습니다.");
        }

        Team team = teamRepository.findById(teamId).orElseThrow(
                () -> new TeamNotFoundException("해당 팀은 찾을 수 없습니다. : " + teamId)
        );

        // 지금 소속팀으로 변경을 시도하는 경우
        if(Objects.equals(member.getTeam().getId(), team.getId())) {
            throw new AlreadySameTeamException("이미 해당 팀에 소속되어 있습니다.");
        }

        member.setTeam(team);

        return new MemberResponse(member);
    }

    @Transactional
    public BookingResponse adjustBooking(Long bookingId, LocalDateTime startTime, LocalDateTime endTime, Long taId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new BookingNotFoundException("해당 예약을 찾을 수 없습니다. : " + bookingId)
        );

        if(!endTime.isAfter(startTime)) {
            throw new InvalidBookingTimeException("종료 시간은 시작 시간보다 늦어야 합니다.");
        }

        booking.setAdjustedAt(startTime, endTime, taId);

        eventPublisher.publishEvent(new NotificationRequestedEvent(bookingId, NotiType.TIME_CHANGED));


        return new BookingResponse(booking.getId(), booking.getRoom().getId(), booking.getStartTime(), booking.getEndTime(), booking.getBookStatus());
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new BookingNotFoundException("해당 예약을 찾을 수 없습니다. : " + bookingId)
        );

        booking.cancelled();

        eventPublisher.publishEvent(new NotificationRequestedEvent(bookingId, NotiType.CANCELLED));

        return new BookingResponse(booking.getId(), booking.getRoom().getId(), booking.getStartTime(), booking.getEndTime(), booking.getBookStatus());
    }

    @Transactional
    public void call(CallRequest req, Long memberId) {
        Member ta = memberRepository.findById(memberId)
                .orElseThrow(()-> new MemberNotFoundException("멤버를 찾을 수 없습니다. memberId: "+memberId));

        // targetId는 targetType에 따라 memberId 또는 teamId라 표시용 이름도 그에 맞게 조회
        String targetName = switch (req.targetType()) {
            case MEMBER -> memberRepository.findById(req.targetId())
                    .orElseThrow(() -> new MemberNotFoundException("memberId가 " + req.targetId() + "인 멤버가 존재하지 않습니다."))
                    .getName();
            case TEAM -> teamRepository.findById(req.targetId())
                    .orElseThrow(() -> new TeamNotFoundException("teamId가 " + req.targetId() + "인 팀이 존재하지 않습니다."))
                    .getName();
        };

        eventPublisher.publishEvent(new CallRequestEvent(req.targetType(), req.targetId(), ta.getId(),
                String.format("[%s] %s 호출: %s", ta.getName(), targetName, req.message())));
    }

    @Transactional
    public ResetPasswordResponse resetPassword(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new MemberNotFoundException("해당 멤버를 찾을 수 없습니다. : " + memberId)
        );

        String newPwd = PasswordGenerator.generate();
        member.setPassword(passwordEncoder.encode(newPwd));
        log.debug("비밀번호 변경 완료 : {}", memberId);

        String message = String.format("[%s] 임시 비밀번호 발급: %s", member.getLoginId(), newPwd);

        try {
            telegramClient.sendMessage(member.getChatId(), message);
        } catch (RestClientException e) {
            log.warn("비밀번호 초기화 텔레그램 전송 실패: {}", member.getLoginId(), e);
            throw new NotificationException("비밀번호 초기화 실패. 다시 시도해주세요.", HttpStatus.BAD_GATEWAY);
        }


        member.setMustChangePassword(true);

        return new ResetPasswordResponse(newPwd);
    }
}
