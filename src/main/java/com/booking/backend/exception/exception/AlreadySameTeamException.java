package com.booking.backend.exception.exception;

public class AlreadySameTeamException extends RuntimeException {
    public AlreadySameTeamException(String message) {
        super(message);
    }
}
