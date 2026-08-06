package com.booking.backend.exception.exception;

import org.springframework.http.HttpStatus;

public class NotificationException extends RuntimeException {
    private final HttpStatus status;

    public NotificationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus(){
        return status;
    }
}
