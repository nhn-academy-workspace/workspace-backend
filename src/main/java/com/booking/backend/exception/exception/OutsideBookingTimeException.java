package com.booking.backend.exception.exception;

public class OutsideBookingTimeException extends RuntimeException {
    public OutsideBookingTimeException(String message) {
        super(message);
    }
}
