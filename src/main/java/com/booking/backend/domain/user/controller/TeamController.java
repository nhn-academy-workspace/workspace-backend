package com.booking.backend.domain.user.controller;

import com.booking.backend.auth.CustomUserDetails;
import com.booking.backend.domain.user.dto.TeamBookingResponse;
import com.booking.backend.domain.user.dto.TeamMemberResponse;
import com.booking.backend.domain.user.dto.TeamUsageResponse;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/v1/teams")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping("/me/members")
    public ResponseEntity<TeamMemberResponse> getMembers(@AuthenticationPrincipal CustomUserDetails userDetails) {

        Member member = userDetails.getMember();

        TeamMemberResponse res = teamService.getTeamMembers(member.getId(), member.getTeam().getId());

        return ResponseEntity.ok(res);
    }

    @GetMapping("/{teamId}/bookings")
    public ResponseEntity<List<TeamBookingResponse>> getTeamBookings(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                               @PathVariable Long teamId) {

        List<TeamBookingResponse> res = teamService.getTeamBookings(userDetails.getMember().getId(), teamId);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{teamId}/usage")
    public ResponseEntity<TeamUsageResponse> getTeamUsage(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                          @PathVariable Long teamId,
                                                          @RequestParam LocalDate date) {

        TeamUsageResponse res = teamService.getTeamUsage(userDetails.getMember().getId(), teamId, date);

        return ResponseEntity.ok(res);
    }



}
