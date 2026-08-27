package com.booking.backend.domain.game.entity;

public enum GameSessionStatus {
    /** 발급됨 — 하트비트 수신 및 점수 제출 가능. */
    ACTIVE,
    /** 정상 제출 완료 — 재사용 불가. */
    SUBMITTED,
    /** 검증 실패(하트비트 끊김, 점수 불일치, 동시 세션 초과 등)로 폐기됨. */
    INVALIDATED,
}
