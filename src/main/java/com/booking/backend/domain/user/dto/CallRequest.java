package com.booking.backend.domain.user.dto;

import com.booking.backend.domain.notification.entity.TargetType;

public record CallRequest(
    TargetType targetType,
    Long targetId,
    String message
) {
}
