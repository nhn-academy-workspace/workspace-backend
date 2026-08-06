package com.booking.backend.exception.exception;

import org.springframework.http.HttpStatus;

public class NotValidSecretTokenException extends NotificationException {
    public NotValidSecretTokenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
