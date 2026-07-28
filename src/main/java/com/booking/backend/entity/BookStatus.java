package com.booking.backend.entity;

public enum BookStatus {

    BOOKED,         // 예약
    CANCELLED,      // 예약 취소
    EARLY_RETURNED, // 반납 (자발적인)
    COMPLETED       // 사용 완료
}
