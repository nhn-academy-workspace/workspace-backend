package com.booking.backend.domain.game.service;

import com.booking.backend.domain.game.config.GameSessionProperties;
import com.booking.backend.domain.game.dto.ScoreRequest;
import com.booking.backend.domain.game.entity.GameSession;
import com.booking.backend.domain.game.entity.GameSessionStatus;
import com.booking.backend.domain.game.entity.Score;
import com.booking.backend.domain.game.repository.ScoreRepository;
import com.booking.backend.domain.game.repository.SessionRepository;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.exception.exception.InvalidGameSessionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 자체 침투 테스트 — 공격자 관점에서 치팅 방어를 뚫어본다.
 * 통과(방어 성공)와 실패(구멍 발견)를 모두 사실대로 기록한다.
 */
class GameCheatAttackTest {

    private static final Long ATTACKER = 1L;
    private static final Long VICTIM = 2L;

    private final ScoreRepository scoreRepository = mock(ScoreRepository.class);
    private final SessionRepository sessionRepository = mock(SessionRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final GameScoreCalculator calc = new GameScoreCalculator();
    private final GameSessionProperties props = new GameSessionProperties(
            Duration.ofMinutes(10), Duration.ofSeconds(5), 2.5, 1.02, 0.70, 3, Duration.ofSeconds(3));

    private final GameService service = new GameService(
            scoreRepository, sessionRepository, memberRepository, calc, props);

    private Member attacker;

    @BeforeEach
    void setUp() {
        attacker = mock(Member.class);
        when(attacker.getId()).thenReturn(ATTACKER);
    }

    // ══ 공격 1. 위조 세션 ID로 제출 ══════════════════════════════

    @Test
    @DisplayName("[방어] 존재하지 않는 세션 ID로는 제출할 수 없다")
    void forgedSessionId() {
        when(sessionRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(ATTACKER, new ScoreRequest("아무거나", 99999L)))
                .isInstanceOf(InvalidGameSessionException.class)
                .hasMessageContaining("유효하지 않은");
    }

    // ══ 공격 2. 세션 발급 후 방치했다 고득점 제출 ═════════════════

    @Test
    @DisplayName("[방어] 8분 방치 후 4000점 제출 → 하트비트 부족으로 반려")
    void idleThenSubmit() {
        GameSession s = session(480);
        give(s);

        assertThatThrownBy(() -> service.submit(ATTACKER, new ScoreRequest(s.getId(), 4000L)))
                .isInstanceOf(InvalidGameSessionException.class);

        assertThat(s.getStatus()).isEqualTo(GameSessionStatus.INVALIDATED);
        verify(scoreRepository, never()).save(any(Score.class));
    }

    // ══ 공격 3. 하트비트를 몰아서 보낸 뒤 고득점 제출 ═════════════

    @Test
    @DisplayName("[방어] 하트비트를 순식간에 200번 보내도 점수 상한은 실제 경과 시간에 묶인다")
    void beatFlooding() {
        GameSession s = session(10);
        ReflectionTestUtils.setField(s, "beatCount", 200);
        ReflectionTestUtils.setField(s, "lastBeatAt", LocalDateTime.now());
        ReflectionTestUtils.setField(s, "lastScore", 50L);
        give(s);

        // 10초 경과인데 1000점을 노림
        assertThatThrownBy(() -> service.submit(ATTACKER, new ScoreRequest(s.getId(), 1000L)))
                .isInstanceOf(InvalidGameSessionException.class)
                .hasMessageContaining("일치하지 않습니다");
    }

    // ══ 공격 4. 타인 세션 도용 / 재사용 ═══════════════════════════

    @Test
    @DisplayName("[방어] 남의 세션으로는 제출할 수 없다")
    void stealSession() {
        Member victim = mock(Member.class);
        when(victim.getId()).thenReturn(VICTIM);
        GameSession s = new GameSession(victim);
        ReflectionTestUtils.setField(s, "startedAt", LocalDateTime.now().minusSeconds(60));
        give(s);

        assertThatThrownBy(() -> service.submit(ATTACKER, new ScoreRequest(s.getId(), 300L)))
                .isInstanceOf(InvalidGameSessionException.class)
                .hasMessageContaining("본인의 세션이 아닙니다");
    }

    @Test
    @DisplayName("[방어] 한 세션으로 두 번 제출할 수 없다")
    void replaySession() {
        GameSession s = session(60);
        played(s, 60, 12);
        give(s);

        service.submit(ATTACKER, new ScoreRequest(s.getId(), (long) calc.expectedScore(60)));

        assertThatThrownBy(() -> service.submit(ATTACKER, new ScoreRequest(s.getId(), 300L)))
                .isInstanceOf(InvalidGameSessionException.class)
                .hasMessageContaining("이미 사용된 세션");
    }

    // ══ 공격 5. 무효화된 세션으로 재시도 ══════════════════════════

    @Test
    @DisplayName("[방어] 한 번 반려당한 세션은 값을 낮춰 재시도해도 막힌다")
    void retryAfterRejection() {
        GameSession s = session(60);
        played(s, 60, 12);
        give(s);

        // 1차: 과도한 점수 → 반려되며 세션 폐기
        catchThrowable(() -> service.submit(ATTACKER, new ScoreRequest(s.getId(), 99999L)));

        // 2차: 정상 범위로 낮춰 재시도
        assertThatThrownBy(() -> service.submit(ATTACKER, new ScoreRequest(s.getId(), 366L)))
                .isInstanceOf(InvalidGameSessionException.class)
                .hasMessageContaining("무효화된 세션");
    }

    // ══ 공격 6. 점수 곡선을 흉내낸 가짜 하트비트 (알려진 한계) ════

    @Test
    @DisplayName("[뚫림] 점수 곡선을 재현한 실시간 봇은 통과한다 — 설계상 한계")
    void scriptedFakeHeartbeats() {
        // 공격자가 네트워크 탭으로 곡선을 관측해 그대로 흉내낸 상황
        GameSession s = session(300);
        played(s, 300, 59);
        give(s);

        long score = (long) calc.expectedScore(300);
        service.submit(ATTACKER, new ScoreRequest(s.getId(), score));

        assertThat(s.getStatus()).isEqualTo(GameSessionStatus.SUBMITTED);
        verify(scoreRepository).save(any(Score.class));
        // → 리플레이 검증 없이는 막을 수 없음. 단 실제 시간을 그대로 소모해야 한다.
    }

    // ══ 공격 7. 초단기 세션 반복 (플레이 횟수 농사) ═══════════════

    @Test
    @DisplayName("[뚫림] 2초짜리 세션은 하트비트 0번으로도 통과한다 — 플레이 횟수 랭킹 농사 가능")
    void shortSessionFarming() {
        GameSession s = session(2);   // 하트비트 한 번도 안 보냄
        give(s);

        // expected(2) ≈ 10.9 → 허용 밴드 [7.6, 12.1]
        service.submit(ATTACKER, new ScoreRequest(s.getId(), 10L));

        assertThat(s.getStatus()).isEqualTo(GameSessionStatus.SUBMITTED);
        verify(scoreRepository).save(any(Score.class));
        // → 점수는 못 부풀리지만 scores 행이 쌓여 "가장 많이 플레이한 사람" 랭킹이 오염된다.
    }

    // ══ 공격 8. TTL 우회 ═════════════════════════════════════════

    @Test
    @DisplayName("[방어] TTL을 넘긴 세션은 완벽한 하트비트 기록이 있어도 만료된다")
    void ttlBypass() {
        GameSession s = session(700);
        played(s, 700, 139);
        give(s);

        assertThatThrownBy(() -> service.submit(ATTACKER, new ScoreRequest(s.getId(), 5000L)))
                .isInstanceOf(InvalidGameSessionException.class)
                .hasMessageContaining("만료된 세션");
    }

    // ══ 공격 9. 음수 / null 점수로 검증 우회 ══════════════════════

    @Test
    @DisplayName("[방어] 음수 점수는 반려된다")
    void negativeScore() {
        GameSession s = session(60);
        played(s, 60, 12);
        give(s);

        assertThatThrownBy(() -> service.submit(ATTACKER, new ScoreRequest(s.getId(), -100L)))
                .isInstanceOf(InvalidGameSessionException.class);
    }

    @Test
    @DisplayName("[방어] null 점수는 반려된다")
    void nullScore() {
        GameSession s = session(60);
        played(s, 60, 12);
        give(s);

        assertThatThrownBy(() -> service.submit(ATTACKER, new ScoreRequest(s.getId(), null)))
                .isInstanceOf(InvalidGameSessionException.class);
    }

    // ══ 공격 10. 허용 오차 최대치 악용 ════════════════════════════

    @Test
    @DisplayName("[허용] 부풀릴 수 있는 최대치가 6% 이내인지 못 박는다")
    void toleranceAbuse() {
        // 허용 오차(1.02)와 발급 지연 보정(startGrace)은 정상 플레이어가 반려되지 않도록
        // 반드시 필요하다. 대신 그만큼 부풀릴 여지가 생기므로, 그 크기가 조용히 커지지
        // 않도록 여기서 상한을 고정한다.
        //
        // 지연 보정을 빼면 왕복 지연 500ms만으로도 첫 하트비트가 반려된다(GameSessionLatencyTest).
        // 정상 유저를 거르는 것보다 몇 % 인플레를 감수하는 편이 낫다는 판단.
        double honest = calc.expectedScore(120);

        long maxClaimable = binarySearchMaxAccepted(120);
        double inflation = (maxClaimable - honest) / honest;

        assertThat(inflation)
                .as("부풀리기 가능 폭 (정직한 점수 %.0f → 최대 %d)", honest, maxClaimable)
                .isLessThan(0.06);
    }

    /** 경과 시간 elapsed에서 서버가 받아주는 최대 점수를 찾는다. */
    private long binarySearchMaxAccepted(double elapsed) {
        long lo = 0;
        long hi = 100_000;
        while (lo < hi) {
            long mid = (lo + hi + 1) / 2;
            GameSession s = session((long) elapsed);
            played(s, elapsed, (int) (elapsed / 5) - 1);
            give(s);
            if (catchThrowable(() -> service.submit(ATTACKER, new ScoreRequest(s.getId(), mid))) == null) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo;
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────

    private GameSession session(long secondsAgo) {
        GameSession s = new GameSession(attacker);
        ReflectionTestUtils.setField(s, "startedAt", LocalDateTime.now().minusSeconds(secondsAgo));
        return s;
    }

    private void played(GameSession s, double elapsed, int beats) {
        ReflectionTestUtils.setField(s, "beatCount", beats);
        ReflectionTestUtils.setField(s, "lastScore", (long) calc.expectedScore(elapsed));
        ReflectionTestUtils.setField(s, "lastBeatAt", LocalDateTime.now());
    }

    private void give(GameSession s) {
        when(sessionRepository.findById(s.getId())).thenReturn(Optional.of(s));
    }
}
