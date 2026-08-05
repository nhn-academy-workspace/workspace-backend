package com.booking.backend.exception.exception;

import org.springframework.http.HttpStatus;

public class ReminderMessageException extends NotificationException {
    public ReminderMessageException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
