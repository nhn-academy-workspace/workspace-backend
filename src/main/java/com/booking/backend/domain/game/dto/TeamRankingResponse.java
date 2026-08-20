package com.booking.backend.domain.game.dto;

import java.math.BigDecimal;

public record TeamRankingResponse(
       String teamName,
       BigDecimal avgScore
) {
}
