package com.booking.backend.domain.notification.exception;

public class NotValidBotUsernameException extends RuntimeException {
    public NotValidBotUsernameException(String message) {
        super(message);
    }
}
