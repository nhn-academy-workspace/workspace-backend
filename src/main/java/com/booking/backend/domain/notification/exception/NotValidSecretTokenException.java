package com.booking.backend.domain.notification.exception;

// 403
public class NotValidSecretTokenException extends RuntimeException {
    public NotValidSecretTokenException(String message) {
        super(message);
    }
}
