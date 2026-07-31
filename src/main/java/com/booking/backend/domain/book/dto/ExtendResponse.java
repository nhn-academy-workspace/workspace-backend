package com.booking.backend.domain.book.dto;

import com.booking.backend.domain.book.entity.BookStatus;

import java.time.LocalDateTime;

public record ExtendResponse(
        Long bookingId,
        Long roomId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BookStatus status
) {
}
