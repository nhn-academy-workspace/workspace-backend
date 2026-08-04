package com.booking.backend.exception.exception;

public class InvalidLockTimeException extends RuntimeException {
    public InvalidLockTimeException(String message) {
        super(message);
    }
}
