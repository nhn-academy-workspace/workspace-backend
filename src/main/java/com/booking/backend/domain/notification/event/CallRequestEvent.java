package com.booking.backend.domain.notification.event;

import com.booking.backend.domain.notification.entity.TargetType;

// 호출 알림
public record CallRequestEvent(
        TargetType targetType,
        Long targetId,
        String message
) {
}
