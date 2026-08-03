package com.booking.backend.domain.user.dto;

import com.booking.backend.domain.user.entity.Member;

import java.util.List;

public record AllMemberResponse(
        Long teamId,
        String name,
        List<MemberInfo> members
) {

    public record MemberInfo(
            Long memberId,
            String name,
            String loginId
    ) {
        public MemberInfo(Member member) {
            this(member.getId(), member.getName(), member.getLoginId());
        }
    }



}
