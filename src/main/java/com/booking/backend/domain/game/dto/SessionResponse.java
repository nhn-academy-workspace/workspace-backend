package com.booking.backend.domain.game.dto;

/**
 * @param sessionId      발급된 게임 세션 UUID
 * @param beatIntervalMs 클라이언트가 하트비트를 보내야 하는 주기(ms).
 *                       서버가 내려줘서 클라이언트 하드코딩과 이중 관리를 피한다.
 */
public record SessionResponse(
        String sessionId,
        long beatIntervalMs
) {
}
