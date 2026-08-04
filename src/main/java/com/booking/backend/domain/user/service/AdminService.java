package com.booking.backend.domain.user.service;

import com.booking.backend.domain.book.dto.BookingResponse;
import com.booking.backend.domain.book.entity.Booking;
import com.booking.backend.domain.book.repository.BookingRepository;
import com.booking.backend.domain.user.dto.MemberResponse;
import com.booking.backend.domain.user.dto.TeamResponse;
import com.booking.backend.domain.user.dto.CallRequest;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.entity.Role;
import com.booking.backend.domain.user.entity.Team;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.domain.user.repository.TeamRepository;
import com.booking.backend.exception.exception.AlreadySameTeamException;
import com.booking.backend.exception.exception.BookingNotFoundException;
import com.booking.backend.exception.exception.InvalidBookingMemberException;
import com.booking.backend.exception.exception.InvalidBookingTimeException;
import com.booking.backend.exception.exception.MemberNotFoundException;
import com.booking.backend.exception.exception.TeamNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            throw new InvalidBookingMemberException("수강생만 팀을 재배정할 수 있습니다.");
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

        // TODO TA에 의한 예약 조정 알림

        return new BookingResponse(booking.getId(), booking.getRoom().getId(), booking.getStartTime(), booking.getEndTime(), booking.getBookStatus());
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new BookingNotFoundException("해당 예약을 찾을 수 없습니다. : " + bookingId)
        );

        booking.cancelled();

        // TODO TA에 의한 예약 취소 알림

        return new BookingResponse(booking.getId(), booking.getRoom().getId(), booking.getStartTime(), booking.getEndTime(), booking.getBookStatus());
    }

    @Transactional // 여기도 트랜잭션 달아야함?
    public void call(CallRequest req) {
        // TODO 호출 알림
    }
}
