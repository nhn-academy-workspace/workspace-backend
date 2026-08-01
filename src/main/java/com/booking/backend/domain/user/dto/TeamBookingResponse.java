package com.booking.backend.domain.user.dto;

import com.booking.backend.domain.book.entity.BookStatus;
import com.booking.backend.domain.book.entity.Booking;

import java.time.LocalDateTime;

public record TeamBookingResponse(
        Long bookingId,
        String roomName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BookStatus status,
        LocalDateTime originalStartTime,
        LocalDateTime originalEndTime
) {
    public TeamBookingResponse(Booking booking) {
        this(booking.getId(),
                booking.getRoom().getName(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getBookStatus(),
                booking.getOriginalStartTime(),
                booking.getOriginalEndTime());
    }
}
