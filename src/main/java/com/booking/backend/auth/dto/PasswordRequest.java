package com.booking.backend.auth.dto;

public record PasswordRequest(
        String currentPassword,
        String newPassword
) {
}
