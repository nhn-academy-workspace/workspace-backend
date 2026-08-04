package com.booking.backend.domain.user.dto;

import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.entity.Role;

public record MemberResponse(
        Long memberId,
        String name,
        String loginId,
        Role role,
        String teamName
) {
    public MemberResponse(Member member) {
        this(member.getId(), member.getName(), member.getLoginId(), member.getRole(), member.getTeam().getName());
    }
}
