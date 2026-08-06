package com.booking.backend.domain.notification.event;

import com.booking.backend.domain.notification.entity.NotiType;

// 예약 기반 알림(START_REMINDER/END_REMINDER/CANCELLED/TIME_CHANGED)
public record NotificationRequestedEvent(
        Long bookingId,
        NotiType notiType
) {
}
