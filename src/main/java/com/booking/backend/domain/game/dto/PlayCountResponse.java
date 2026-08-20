package com.booking.backend.domain.game.dto;

public record PlayCountResponse(
        String memberName,
        String teamName,
        Long playCount
) {
}
