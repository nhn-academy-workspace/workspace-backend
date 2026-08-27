package com.booking.backend.domain.game.service;

import com.booking.backend.domain.game.config.GameSessionProperties;
import com.booking.backend.domain.game.dto.ScoreRequest;
import com.booking.backend.domain.game.entity.GameSession;
import com.booking.backend.domain.game.entity.GameSessionStatus;
import com.booking.backend.domain.game.repository.ScoreRepository;
import com.booking.backend.domain.game.repository.SessionRepository;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.exception.exception.InvalidGameSessionException;
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
 * 정상 플레이어가 반려되는지 확인한다.
 *
 * 클라이언트는 "게임 시간" 기준으로 하트비트를 보내는데(시뮬레이션이 진행된 만큼만 누적),
 * 서버는 "실제 경과 시간" 기준으로 개수를 기대한다. 둘 사이에 드리프트가 생기면
 * 아무 잘못 없는 플레이어가 반려된다.
 *
 * 드리프트 원인: 세션 발급 왕복 지연, 프레임 히칭(MAX_DT 초과분 폐기), GC, rAF 스로틀링.
 */
class GameBeatDriftTest {

    private static final Long PLAYER = 1L;

    private final ScoreRepository scoreRepository = mock(ScoreRepository.class);
    private final SessionRepository sessionRepository = mock(SessionRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final GameScoreCalculator calc = new GameScoreCalculator();
    private final GameSessionProperties props = new GameSessionProperties(
            Duration.ofMinutes(10), Duration.ofSeconds(5), 2.5, 1.02, 0.70, 3);

    private final GameService service = new GameService(
            scoreRepository, sessionRepository, memberRepository, calc, props);

    private Member player;

    @BeforeEach
    void setUp() {
        player = mock(Member.class);
        when(player.getId()).thenReturn(PLAYER);
    }

    /**
     * 정직한 플레이어를 시뮬레이션한다.
     *
     * @param playSeconds  실제 플레이한 초
     * @param latencyMs    세션 발급 왕복 지연 — 이만큼 첫 하트비트가 늦게 시작된다
     * @param driftPercent 프레임 손실률 — 게임 시간이 실제 시간보다 이만큼 뒤처진다
     */
    @ParameterizedTest(name = "{0}초 플레이 / 지연 {1}ms / 드리프트 {2}% → 저장 {3}")
    @CsvSource({
            // 이상적인 환경 (드리프트 없음)
            " 60,   0, 0, true",
            "120,   0, 0, true",
            // 현실적인 네트워크 지연만 있는 경우
            " 60, 300, 0, true",
            "120, 300, 0, true",
            // 가벼운 프레임 손실 — 흔한 수준
            " 60, 300, 3, true",
            "100, 300, 5, true",
            "120, 300, 5, true",
            "180, 300, 5, true",
            // 중간 정도 손실
            "120, 300, 10, true",
            "180, 500, 10, true",
            // 저사양 기기 — 심한 프레임 손실
            "120, 800, 20, true",
            "300, 800, 25, true",
            // 세션 발급이 아주 느린 환경
            "120, 2000, 10, true",
            // 하트비트를 아예 안 보내는 클라이언트(구버전)는 반려돼야 한다
            "480,   0, 100, false",
    })
    @DisplayName("정상 플레이어는 어떤 환경에서도 반려되면 안 된다")
    void honestPlayerMustNotBeRejected(double playSeconds, long latencyMs,
                                       double driftPercent, boolean shouldSave) {

        GameSession session = honestPlayer(playSeconds, latencyMs, driftPercent);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        long finalScore = (long) calc.expectedScore(playSeconds * (1 - driftPercent / 100.0));
        Throwable thrown = catchThrowable(
                () -> service.submit(PLAYER, new ScoreRequest(session.getId(), finalScore)));

        if (shouldSave) {
            assertThat(thrown)
                    .as("정상 플레이(%.0f초, 지연 %dms, 드리프트 %.0f%%)가 반려됨: %s",
                            playSeconds, latencyMs, driftPercent,
                            thrown == null ? "" : thrown.getMessage())
                    .isNull();
            assertThat(session.getStatus()).isEqualTo(GameSessionStatus.SUBMITTED);
        } else {
            assertThat(thrown).isInstanceOf(InvalidGameSessionException.class);
        }
    }

    /**
     * 클라이언트 동작을 그대로 재현한다.
     * 하트비트는 "게임 시간" 5초마다 발사되므로, 드리프트가 있으면 실제 간격은 그보다 길어진다.
     */
    private GameSession honestPlayer(double playSeconds, long latencyMs, double driftPercent) {
        GameSession s = new GameSession(player);
        LocalDateTime startedAt = LocalDateTime.now().minusNanos((long) (playSeconds * 1_000_000_000L));
        ReflectionTestUtils.setField(s, "startedAt", startedAt);

        double beatWallInterval = 5.0 * (1 + driftPercent / 100.0);  // 실제 간격
        double firstBeatAt = latencyMs / 1000.0 + beatWallInterval;

        int beats = 0;
        double lastBeatWall = 0;
        for (double t = firstBeatAt; t <= playSeconds; t += beatWallInterval) {
            beats++;
            lastBeatWall = t;
        }

        ReflectionTestUtils.setField(s, "beatCount", beats);
        ReflectionTestUtils.setField(s, "lastScore",
                (long) calc.expectedScore(lastBeatWall * (1 - driftPercent / 100.0)));
        ReflectionTestUtils.setField(s, "lastBeatAt",
                startedAt.plusNanos((long) (lastBeatWall * 1_000_000_000L)));
        return s;
    }
}
