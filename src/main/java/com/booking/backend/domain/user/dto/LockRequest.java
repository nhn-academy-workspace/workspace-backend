package com.booking.backend.domain.user.dto;

import java.time.LocalDateTime;

public record LockRequest(
        Long roomId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String reason
) {
}
