package com.booking.backend.exception.exception;

public class BookingTimeLimitExceedException extends RuntimeException {
    public BookingTimeLimitExceedException(String message) {
        super(message);
    }
}
