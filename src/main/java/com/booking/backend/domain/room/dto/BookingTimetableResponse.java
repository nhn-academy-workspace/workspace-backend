package com.booking.backend.domain.room.dto;

import com.booking.backend.domain.book.Booking;
import com.booking.backend.domain.room.RoomLock;

import java.time.LocalDateTime;

public record BookingTimetableResponse(
        Long id,
        String type,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String teamName, // Booking일 경우 얘만 채워짐
        String reason    // Lock일 경우 얘만 채워짐
) {

    public BookingTimetableResponse(Booking booking) {
        this(booking.getId(), "BOOKING", booking.getStartTime(), booking.getEndTime(), booking.getTeam().getName(), null);
    }
    public BookingTimetableResponse(RoomLock roomLock) {
        this(roomLock.getId(), "LOCK", roomLock.getStartTime(), roomLock.getEndTime(), null, roomLock.getReason());
    }
}
