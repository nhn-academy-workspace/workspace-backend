package com.booking.backend.exception.exception;

import org.springframework.http.HttpStatus;

public class InsufficientPermissionException extends NotificationException {
    public InsufficientPermissionException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
