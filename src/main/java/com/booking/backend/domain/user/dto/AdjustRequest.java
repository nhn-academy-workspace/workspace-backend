package com.booking.backend.domain.user.dto;

import java.time.LocalDateTime;

public record AdjustRequest(
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
