package com.booking.backend.domain.notification.dto;

import com.booking.backend.domain.notification.entity.NotiType;
import com.booking.backend.domain.notification.entity.Notification;
import com.booking.backend.domain.notification.entity.NotificationStatus;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotiType notiType,
        String message,
        NotificationStatus status,
        LocalDateTime sentAt,
        LocalDateTime readAt
) {
    public NotificationResponse fromEntity(Notification notification){
        return new NotificationResponse(
                notification.getId(),
                notification.getNotiType(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getSentAt(),
                notification.getReadAt()
        );
    }
}
