package com.booking.backend.domain.notification.dto;

import com.booking.backend.domain.notification.NotiType;
import com.booking.backend.domain.notification.NotificationStatus;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotiType notiType,
        String message,
        NotificationStatus status,
        LocalDateTime sentAt,
        LocalDateTime readAt
) {
}
