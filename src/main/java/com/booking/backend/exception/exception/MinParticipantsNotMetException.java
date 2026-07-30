package com.booking.backend.exception.exception;

public class MinParticipantsNotMetException extends RuntimeException {
    public MinParticipantsNotMetException(String message) {
        super(message);
    }
}
