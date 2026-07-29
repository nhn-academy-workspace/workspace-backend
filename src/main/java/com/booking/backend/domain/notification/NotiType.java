package com.booking.backend.domain.notification;

public enum NotiType {
    START_REMINDER, // 시작 알림
    END_REMINDER,   // 종료 알림
    CANCELLED,      // 취소 알림
    TIME_CHANGED,   // 시간 조정 알림
    CALL            // 호출 알림
}
