package com.booking.backend.domain.game.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 게임 세션 치팅 방어 파라미터.
 * 운영 중 튜닝할 수 있도록 application.yml(game.session.*)로 분리한다.
 *
 * @param ttl                   세션 최대 수명. 초과 시 제출 불가 — 방치형 치팅의 획득 상한을 고정한다.
 * @param beatInterval          클라이언트 하트비트 주기.
 * @param beatGapTolerance      허용 gap 배수. beatInterval * 이 값을 넘게 끊기면 세션 무효화.
 * @param scoreUpperTolerance   기대 점수 대비 상한 배수(반올림/부분 스텝 오차 흡수).
 * @param scoreLowerTolerance   기대 점수 대비 하한 배수(시간 부풀리기 차단). 저사양 기기
 *                              오탐을 피하려 느슨하게 잡는다.
 * @param maxActiveSessions     사용자당 동시 활성 세션 수. 초과 발급 시 오래된 것부터 무효화.
 */
@ConfigurationProperties(prefix = "game.session")
public record GameSessionProperties(
        Duration ttl,
        Duration beatInterval,
        double beatGapTolerance,
        double scoreUpperTolerance,
        double scoreLowerTolerance,
        int maxActiveSessions
) {

    public GameSessionProperties {
        if (ttl == null) ttl = Duration.ofMinutes(10);
        if (beatInterval == null) beatInterval = Duration.ofSeconds(5);
        if (beatGapTolerance <= 0) beatGapTolerance = 2.5;
        if (scoreUpperTolerance <= 0) scoreUpperTolerance = 1.02;
        if (scoreLowerTolerance <= 0) scoreLowerTolerance = 0.70;
        if (maxActiveSessions <= 0) maxActiveSessions = 3;
    }

    /** 하트비트 사이 허용되는 최대 공백. */
    public Duration maxBeatGap() {
        return Duration.ofMillis((long) (beatInterval.toMillis() * beatGapTolerance));
    }
}
