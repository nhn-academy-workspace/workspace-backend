package com.booking.backend.exception.exception;

public class TelegramNotLinkedException extends RuntimeException {
    public TelegramNotLinkedException(String message) {
        super(message);
    }
}
