package com.booking.backend.exception.exception;

public class InvalidBookingMemberException extends RuntimeException {
    public InvalidBookingMemberException(String message) {
        super(message);
    }
}
