package com.booking.backend.exception.exception;

public class InvalidNewPasswordException extends RuntimeException {
    public InvalidNewPasswordException(String message) {
        super(message);
    }
}
