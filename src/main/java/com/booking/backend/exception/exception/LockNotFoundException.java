package com.booking.backend.exception.exception;

public class LockNotFoundException extends RuntimeException {
    public LockNotFoundException(String message) {
        super(message);
    }
}
