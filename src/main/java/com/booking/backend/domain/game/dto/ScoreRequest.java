package com.booking.backend.domain.game.dto;

public record ScoreRequest(
        String sessionId,
        Long score
) {
}
