package com.booking.backend.domain.book.dto;

import com.booking.backend.domain.book.entity.BookStatus;

public record EarlyReturnResponse(
        BookStatus status
) {
}
