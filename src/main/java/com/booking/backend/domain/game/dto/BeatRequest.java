package com.booking.backend.domain.game.dto;

/** 플레이 중 주기적으로 보고하는 진행 상황. */
public record BeatRequest(
        Long score
) {
}
