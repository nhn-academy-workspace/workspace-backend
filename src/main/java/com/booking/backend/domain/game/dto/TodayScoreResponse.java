package com.booking.backend.domain.game.dto;

import com.booking.backend.domain.game.entity.Score;

public record TodayScoreResponse(
        String memberName,
        String teamName,
        Long score
) {
    public TodayScoreResponse(Score score) {
        this(score.getMember().getName(), score.getMember().getTeam() != null ? score.getMember().getTeam().getName() : "TA", score.getGameScore());
    }
}
