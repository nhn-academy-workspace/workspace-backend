package com.booking.backend.domain.book.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BookingRequest(
        Long roomId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        List<Long> memberIds     // 회의에 소속된 멤버의 id
) {
}
