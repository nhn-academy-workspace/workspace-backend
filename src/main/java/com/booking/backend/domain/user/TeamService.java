package com.booking.backend.domain.user;

import com.booking.backend.domain.user.dto.TeamMemberResponse;
import com.booking.backend.domain.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class TeamService {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public TeamMemberResponse getTeamMembers(Long memberId, Long teamId) {

        List<Member> memberList= memberRepository.findByTeamId(teamId);

        List<TeamMemberResponse.MemberInfo> memberInfos = memberList.stream()
                .map(member -> new TeamMemberResponse.MemberInfo(member.getId(), member.getName()))
                .toList();

        return new TeamMemberResponse(memberId, memberInfos);
    }
}
