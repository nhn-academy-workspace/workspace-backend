package com.booking.backend.domain.user.dto;

import java.util.List;

public record TeamMemberResponse(
        Long memberId,
        List<MemberInfo> members
) {
    public record MemberInfo(
        Long memberId,
        String name
    ) {}
}
