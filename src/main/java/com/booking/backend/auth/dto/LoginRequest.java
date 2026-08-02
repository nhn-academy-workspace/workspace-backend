package com.booking.backend.auth.dto;


public record LoginRequest(
        String loginId,
        String password
) {
}
