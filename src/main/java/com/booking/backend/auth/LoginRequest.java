package com.booking.backend.auth;


public record LoginRequest(
        String loginId,
        String password
) {
}
