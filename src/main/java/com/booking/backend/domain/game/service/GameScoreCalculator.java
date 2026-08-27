package com.booking.backend.domain.game.service;

import org.springframework.stereotype.Component;

/**
 * 경과 시간으로부터 "정상 플레이 시 나올 수 있는 점수"를 계산한다.
 *
 * 이 게임의 점수는 생존 시간의 결정론적 함수라서 서버가 그대로 재현할 수 있고,
 * 덕분에 상한(과다 점수)뿐 아니라 하한(시간 부풀리기)까지 검증할 수 있다.
 *
 * ⚠️ 아래 상수는 프론트엔드 DinoGamePage.tsx와 반드시 일치해야 한다.
 *    어긋나면 정상 플레이어의 점수가 전부 반려된다.
 *    GameScoreCalculatorTest가 고정값으로 드리프트를 잡아준다.
 */
@Component
public class GameScoreCalculator {

    /** 논리 시뮬레이션 스텝/초 (주사율 무관 고정 스텝). */
    public static final int STEP_HZ = 75;
    /** 스텝당 기본 적립. */
    private static final double SCORE_RATE = 0.072;
    /** 속도 보너스 계수. */
    private static final double SCORE_SPEED_BONUS = 0.2;
    /** 단계당 스텝 수. */
    private static final int STAGE_FRAMES = 600;

    private static final double[] STAGE_SPEEDS = {
            4.0, 4.8, 5.5, 6.2, 6.9,
            7.5, 8.0, 8.5, 9.0, 10.0,
            11.0, 12.0, 12.8, 13.5, 14.0,
    };
    private static final int STAGE_COUNT = STAGE_SPEEDS.length;
    /** 이 스텝 이후로는 속도가 최고치에 고정된다. */
    private static final long CAP_STEP = (long) (STAGE_COUNT - 1) * STAGE_FRAMES;

    /**
     * 경과 초에 대한 기대 점수.
     *
     * 각 단계 내에서 속도가 프레임에 선형이므로 스텝별 적립도 선형이다.
     * 따라서 단계별 합은 등차수열 합으로 정확히 구할 수 있다(루프 15회면 끝).
     */
    public double expectedScore(double seconds) {
        if (seconds <= 0) {
            return 0;
        }
        long totalSteps = (long) (seconds * STEP_HZ);
        double score = 0;

        for (int s = 0; s < STAGE_COUNT - 1; s++) {
            long stageStart = (long) s * STAGE_FRAMES;
            if (totalSteps <= stageStart) {
                break;
            }
            long n = Math.min(totalSteps - stageStart, STAGE_FRAMES);
            double from = STAGE_SPEEDS[s];
            double delta = STAGE_SPEEDS[s + 1] - from;
            // local = 0..n-1 구간의 평균 속도
            double avgSpeed = from + delta * ((n - 1) / 2.0) / STAGE_FRAMES;
            score += n * stepScore(avgSpeed);
        }

        if (totalSteps > CAP_STEP) {
            score += (totalSteps - CAP_STEP) * stepScore(STAGE_SPEEDS[STAGE_COUNT - 1]);
        }

        return score;
    }

    /** 기대 점수의 상한(허용 오차 적용). */
    public double upperBound(double seconds, double tolerance) {
        return expectedScore(seconds) * tolerance + 1;
    }

    /** 기대 점수의 하한(허용 오차 적용). */
    public double lowerBound(double seconds, double tolerance) {
        return expectedScore(seconds) * tolerance;
    }

    private double stepScore(double speed) {
        return SCORE_RATE * (1 + (speed - STAGE_SPEEDS[0]) / STAGE_SPEEDS[0] * SCORE_SPEED_BONUS);
    }
}
