package com.booking.backend.domain.game.service;

import com.booking.backend.domain.game.config.GameSessionProperties;
import com.booking.backend.domain.game.entity.GameSession;
import com.booking.backend.domain.game.repository.ScoreRepository;
import com.booking.backend.domain.game.repository.SessionRepository;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 세션 발급 왕복 지연이 첫 하트비트를 반려시키는지 확인한다.
 *
 * 클라이언트는 POST /sessions를 쏘자마자 게임을 시작한다(응답을 기다리지 않음).
 * 그런데 서버의 startedAt은 요청이 도착해 저장되는 시점에 찍힌다.
 * 따라서 클라이언트 게임 시간이 서버 경과 시간보다 항상 지연(L)만큼 앞선다.
 *
 * 이 오차는 세션 내내 고정인데, 상한 여유(expected × 0.02 + 1)는 초반에 매우 작다.
 * → 첫 하트비트가 가장 취약하다.
 */
class GameSessionLatencyTest {

    private static final Long PLAYER = 1L;

    private final ScoreRepository scoreRepository = mock(ScoreRepository.class);
    private final SessionRepository sessionRepository = mock(SessionRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final GameScoreCalculator calc = new GameScoreCalculator();
    private final GameSessionProperties props = new GameSessionProperties(
            Duration.ofMinutes(10), Duration.ofSeconds(5), 2.5, 1.02, 0.70, 3, Duration.ofSeconds(3));

    private final GameService service = new GameService(
            scoreRepository, sessionRepository, memberRepository, calc, props);

    private Member player;

    @BeforeEach
    void setUp() {
        player = mock(Member.class);
        when(player.getId()).thenReturn(PLAYER);
    }

    @ParameterizedTest(name = "세션 발급 지연 {0}ms → 첫 하트비트 통과 {1}")
    @CsvSource({
            "  0, true",
            " 50, true",
            "100, true",
            "200, true",
            "300, true",
            "500, true",
            "800, true",
            "1500, true",
            "2500, true",
    })
    @DisplayName("세션 발급이 느려도 첫 하트비트는 통과해야 한다")
    void firstBeatSurvivesSessionLatency(long latencyMs, boolean shouldPass) {

        double latencySec = latencyMs / 1000.0;
        // 서버 기준 경과 5초 시점에 첫 하트비트가 도착한다
        double serverElapsed = 5.0;
        // 클라이언트는 지연만큼 먼저 게임을 시작했으므로 그만큼 더 진행돼 있다
        long clientScore = (long) calc.expectedScore(serverElapsed + latencySec);

        GameSession s = new GameSession(player);
        ReflectionTestUtils.setField(s, "startedAt",
                LocalDateTime.now().minusNanos((long) (serverElapsed * 1_000_000_000L)));
        when(sessionRepository.findById(s.getId())).thenReturn(Optional.of(s));

        Throwable thrown = catchThrowable(() -> service.beat(PLAYER, s.getId(), clientScore));

        if (shouldPass) {
            assertThat(thrown)
                    .as("지연 %dms에서 첫 하트비트가 반려됨 (클라 점수 %d, 서버 기대 %.1f): %s",
                            latencyMs, clientScore, calc.expectedScore(serverElapsed),
                            thrown == null ? "" : thrown.getMessage())
                    .isNull();
        } else {
            assertThat(thrown).isNotNull();
        }
    }
}
