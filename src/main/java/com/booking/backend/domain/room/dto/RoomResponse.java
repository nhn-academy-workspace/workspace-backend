package com.booking.backend.domain.room.dto;

import com.booking.backend.domain.room.RoomStatus;

public record RoomResponse(
        Long id,
        String name,
        RoomStatus status
) {
}
