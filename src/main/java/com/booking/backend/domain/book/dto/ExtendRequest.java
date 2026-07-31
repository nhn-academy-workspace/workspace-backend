package com.booking.backend.domain.book.dto;


import java.time.LocalDateTime;

public record ExtendRequest(
        LocalDateTime endTime
) {
}
