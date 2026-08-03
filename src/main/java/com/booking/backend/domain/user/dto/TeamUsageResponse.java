package com.booking.backend.domain.user.dto;

public record TeamUsageResponse(
        Long usedMinutes,
        Long remainingMinutes
) {
}
