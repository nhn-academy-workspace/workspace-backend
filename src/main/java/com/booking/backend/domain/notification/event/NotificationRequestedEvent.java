package com.booking.backend.domain.notification.event;

import com.booking.backend.domain.notification.entity.NotiType;

// TODO: CALL 구현 시 재검토 필요
public record NotificationRequestedEvent(
        // nullable, 예약과 무관한 상황 대비
        Long bookingId,
        NotiType notiType
) {
}
