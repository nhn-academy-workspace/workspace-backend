package com.booking.backend.domain.game.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 프론트엔드(DinoGamePage.tsx)의 점수 곡선과 서버 계산이 일치하는지 고정한다.
 * 프론트 튜닝값을 바꾸면 이 테스트가 깨지면서 서버 동기화를 강제한다.
 */
class GameScoreCalculatorTest {

    private final GameScoreCalculator calculator = new GameScoreCalculator();

    /**
     * 기대값은 프론트 루프를 그대로 시뮬레이션해서 뽑은 수치.
     * (STEP_HZ=75, SCORE_RATE=0.072, SCORE_SPEED_BONUS=0.2)
     */
    @ParameterizedTest(name = "{0}초 → 약 {1}점")
    @CsvSource({
            "30,   173",
            "60,   366",
            "90,   579",
            "120,  817",
            "150,  1060",
            "300,  2275",
    })
    @DisplayName("프론트 점수 곡선과 1% 이내로 일치한다")
    void matchesClientCurve(double seconds, double expected) {
        assertThat(calculator.expectedScore(seconds))
                .isCloseTo(expected, org.assertj.core.data.Offset.offset(expected * 0.01));
    }

    @Test
    @DisplayName("0 이하 경과는 0점")
    void zeroOrNegative() {
        assertThat(calculator.expectedScore(0)).isZero();
        assertThat(calculator.expectedScore(-5)).isZero();
    }

    @Test
    @DisplayName("점수는 시간에 대해 단조 증가한다")
    void monotonic() {
        double prev = -1;
        for (double t = 0; t <= 600; t += 0.5) {
            double cur = calculator.expectedScore(t);
            assertThat(cur).isGreaterThanOrEqualTo(prev);
            prev = cur;
        }
    }

    @Test
    @DisplayName("누적 평균 적립은 서버 검증 상한(9점/초)을 절대 넘지 않는다")
    void neverExceedsServerCap() {
        for (double t = 5; t <= 3600; t += 5) {
            double rate = calculator.expectedScore(t) / t;
            assertThat(rate)
                    .as("%.0f초 시점의 누적 평균 적립", t)
                    .isLessThan(9.0);
        }
    }

    @Test
    @DisplayName("최고 단계 도달 후에는 초당 8.1점으로 수렴한다")
    void convergesToCapRate() {
        double delta = calculator.expectedScore(601) - calculator.expectedScore(600);
        assertThat(delta).isCloseTo(8.1, org.assertj.core.data.Offset.offset(0.05));
    }
}
