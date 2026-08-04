package com.booking.backend.domain.user.service;

import com.booking.backend.domain.user.dto.AllMemberResponse;
import com.booking.backend.domain.user.dto.CallRequest;
import com.booking.backend.domain.user.entity.Member;
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

    public List<AllMemberResponse> getAll() {

        List<Member> members = memberRepository.findAllStudent();
        List<Team> teams = teamRepository.findAll();

        Map<Long, Team> teamsById = teams.stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));

        Map<Long, List<Member>> membersByTeam = members.stream()
                .collect(Collectors.groupingBy(m -> m.getTeam().getId()));


        List<AllMemberResponse> res = new ArrayList<>();

        for(Long teamId : teamsById.keySet()) {
            List<AllMemberResponse.MemberInfo> memberInfos = membersByTeam
                    .getOrDefault(teamId, List.of())
                    .stream()
                    .map(AllMemberResponse.MemberInfo::new)
                    .toList();

            res.add(new AllMemberResponse(teamId, teamsById.get(teamId).getName(), memberInfos));
        }

        return res;
    }

    public void call(CallRequest req) {
        // TODO 호출 알림
    }
}
