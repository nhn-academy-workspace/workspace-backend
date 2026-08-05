package com.booking.backend.exception.exception;

import org.springframework.http.HttpStatus;

public class NotificationNotFoundException extends NotificationException {
    public NotificationNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
