package com.booking.backend.domain.user.service;

import com.booking.backend.domain.book.entity.Booking;
import com.booking.backend.domain.book.repository.BookingRepository;
import com.booking.backend.domain.user.dto.TeamBookingResponse;
import com.booking.backend.domain.user.dto.TeamMemberResponse;
import com.booking.backend.domain.user.dto.TeamUsageResponse;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.exception.exception.InvalidBookingMemberException;
import com.booking.backend.exception.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class TeamService {

    private final MemberRepository memberRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public TeamMemberResponse getTeamMembers(Long memberId, Long teamId) {

        List<Member> memberList= memberRepository.findByTeamId(teamId);

        List<TeamMemberResponse.MemberInfo> memberInfos = memberList.stream()
                .map(member -> new TeamMemberResponse.MemberInfo(member.getId(), member.getName()))
                .toList();

        return new TeamMemberResponse(memberId, memberInfos, teamId);
    }

    @Transactional(readOnly = true)
    public List<TeamBookingResponse> getTeamBookings(Long memberId, Long teamId) {

        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new MemberNotFoundException("해당 멤버를 찾을 수 없습니다. : " + memberId)
        );

        if(!Objects.equals(member.getTeam().getId(), teamId)) {
            throw new InvalidBookingMemberException("회원만 조회할 수 있습니다.");
        }

        List<Booking> bookings = bookingRepository.findByTeamId(teamId);

        return bookings.stream()
                .map(TeamBookingResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamUsageResponse getTeamUsage(Long memberId, Long teamId, LocalDate date) {

        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new MemberNotFoundException("해당 멤버를 찾을 수 없습니다. : " + memberId)
        );

        if(!Objects.equals(member.getTeam().getId(), teamId)) {
            throw new InvalidBookingMemberException("소속팀의 멤버만 확인할 수 있습니다");
        }

        List<Booking> bookings = bookingRepository.findBookingsByTeamIdAndDate(teamId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());

        Long totalUsage = bookings.stream()
                .mapToLong(Booking::getBookingDuration)
                .sum();

        return new TeamUsageResponse(totalUsage, 240 - totalUsage);
    }

}








