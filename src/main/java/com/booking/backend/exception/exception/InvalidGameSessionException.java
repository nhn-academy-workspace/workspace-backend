package com.booking.backend.exception.exception;

public class InvalidGameSessionException extends RuntimeException {
    public InvalidGameSessionException(String message) {
        super(message);
    }
}
