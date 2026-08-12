package com.booking.backend.domain.room.dto;

import com.booking.backend.domain.book.entity.Booking;
import com.booking.backend.domain.room.RoomLock;

import java.time.LocalDateTime;
import java.util.List;

public record BookingTimetableResponse(
        Long id,
        String type,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String teamName, // Booking일 경우 얘만 채워짐
        String reason,    // Lock일 경우 얘만 채워짐
        List<String> memberNames
) {

    // 예약인 경우 생성자
    public BookingTimetableResponse(Booking booking) {
        this(booking.getId(), "BOOKING", booking.getStartTime(), booking.getEndTime(), booking.getTeam().getName(), null, booking.getMemberNames());
    }

    // Lock인 경우 생성자
    public BookingTimetableResponse(RoomLock roomLock) {
        this(roomLock.getId(), "LOCK", roomLock.getStartTime(), roomLock.getEndTime(), null, roomLock.getReason(), null);
    }
}
