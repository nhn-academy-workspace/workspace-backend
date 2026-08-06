package com.booking.backend.exception.exception;

import org.springframework.http.HttpStatus;

public class NotValidBotUsernameException extends NotificationException {
    public NotValidBotUsernameException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
