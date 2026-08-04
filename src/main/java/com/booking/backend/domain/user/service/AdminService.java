package com.booking.backend.domain.user.service;

import com.booking.backend.domain.user.dto.MemberResponse;
import com.booking.backend.domain.user.dto.TeamResponse;
import com.booking.backend.domain.user.dto.CallRequest;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.entity.Role;
import com.booking.backend.domain.user.entity.Team;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.domain.user.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;

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

    public List<MemberResponse> getAllMembers() {
        List<Member> members = memberRepository.findAllStudentWithTeam();

        return members.stream()
                .map(MemberResponse::new)
                .toList();
    }

    public void call(CallRequest req) {
        // TODO 호출 알림
    }
}
