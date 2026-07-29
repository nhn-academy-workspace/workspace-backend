package com.booking.backend.auth;

import com.booking.backend.domain.user.Role;

public record LoginResponse(
        String name,
        Role role,
        String teamName
) {
}
