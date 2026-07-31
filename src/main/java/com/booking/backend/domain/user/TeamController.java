package com.booking.backend.domain.user;

import com.booking.backend.auth.CustomUserDetails;
import com.booking.backend.domain.user.dto.TeamMemberResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
