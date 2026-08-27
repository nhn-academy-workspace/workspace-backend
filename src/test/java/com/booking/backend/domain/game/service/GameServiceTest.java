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
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// 하트비트/TTL 기반 치팅 방어 검증.
// 핵심 시나리오는 "세션만 열어두고 방치했다가 큰 점수를 제출하는" 공격이 막히는지.
class GameServiceTest {

    private static final Long MEMBER_ID = 1L;

    private final ScoreRepository scoreRepository = mock(ScoreRepository.class);
    private final SessionRepository sessionRepository = mock(SessionRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);

    // mock이 아니라 실제 인스턴스 — 진짜 점수 곡선을 태워야 하니까
    private final GameScoreCalculator calculator = new GameScoreCalculator();
    private final GameSessionProperties props = new GameSessionProperties(
            Duration.ofMinutes(10), Duration.ofSeconds(5), 2.5, 1.02, 0.70, 3);

    private final GameService gameService = new GameService(
            scoreRepository, sessionRepository, memberRepository, calculator, props);

    private Member member;

    @BeforeEach
    void setUp() {
        member = mock(Member.class);
        when(member.getId()).thenReturn(MEMBER_ID);
    }

    // ── 핵심: 방치형 치팅 차단 ──────────────────────────────────────

    @Test
    void 하트비트_없이_오래_기다렸다_제출하면_반려된다() {
        // 8분 전에 세션만 발급받고 게임은 하지 않은 상태
        GameSession session = sessionStartedSecondsAgo(480);
        given(session);

        // 480초 × 9 = 4320점을 노린 제출
        assertThatThrownBy(() -> gameService.submit(MEMBER_ID, new ScoreRequest(session.getId(), 4000L)))
                .isInstanceOf(InvalidGameSessionException.class)
                .hasMessageContaining("플레이 기록이 충분하지 않습니다");

        assertThat(session.getStatus()).isEqualTo(GameSessionStatus.INVALIDATED);
        verify(scoreRepository, never()).save(any(Score.class));
    }

    @Test
    void 정상적으로_플레이한_기록은_저장된다() {
        // 60초간 5초마다 하트비트를 보내며 플레이한 상태
        GameSession session = sessionStartedSecondsAgo(60);
        playedNormally(session, 60, 12);
        given(session);

        long finalScore = (long) calculator.expectedScore(60);
        gameService.submit(MEMBER_ID, new ScoreRequest(session.getId(), finalScore));

        assertThat(session.getStatus()).isEqualTo(GameSessionStatus.SUBMITTED);
        verify(scoreRepository).save(any(Score.class));
    }

    // ── 세션 상태 ───────────────────────────────────────────────────

    @Test
    void TTL을_넘긴_세션은_만료된다() {
        GameSession session = sessionStartedSecondsAgo(700);
        playedNormally(session, 700, 139);
        given(session);

        assertThatThrownBy(() -> gameService.submit(MEMBER_ID, new ScoreRequest(session.getId(), 100L)))
                .isInstanceOf(InvalidGameSessionException.class)
                .hasMessageContaining("만료된 세션");
    }

    @Test
    void 이미_제출한_세션은_재사용할_수_없다() {
        GameSession session = sessionStartedSecondsAgo(60);
        session.markSubmitted();
        given(session);

        assertThatThrownBy(() -> gameService.submit(MEMBER_ID, new ScoreRequest(session.getId(), 100L)))
                .isInstanceOf(InvalidGameSessionException.class)
                .hasMessageContaining("이미 사용된 세션");
    }

    @Test
    void 남의_세션에는_제출할_수_없다() {
        GameSession session = sessionStartedSecondsAgo(60);
        given(session);

        assertThatThrownBy(() -> gameService.submit(999L, new ScoreRequest(session.getId(), 100L)))
                .isInstanceOf(InvalidGameSessionException.class)
                .hasMessageContaining("본인의 세션이 아닙니다");
    }

    // ── 하트비트 ────────────────────────────────────────────────────

    @Test
    void 하트비트가_끊기면_세션이_무효화된다() {
        GameSession session = sessionStartedSecondsAgo(60);
        // 마지막 하트비트가 30초 전 — 허용 gap(12.5초)을 초과
        session.recordBeat(200L, LocalDateTime.now().minusSeconds(30));
        given(session);

        assertThatThrownBy(() -> gameService.beat(MEMBER_ID, session.getId(), 300L))
                .isInstanceOf(InvalidGameSessionException.class)
                .hasMessageContaining("하트비트가 끊긴");

        assertThat(session.getStatus()).isEqualTo(GameSessionStatus.INVALIDATED);
    }

    @Test
    void 경과_시간에_비해_과도한_점수는_반려된다() {
        GameSession session = sessionStartedSecondsAgo(10);
        session.recordBeat(50L, LocalDateTime.now());
        given(session);

        // 10초 만에 900점은 물리적으로 불가능
        assertThatThrownBy(() -> gameService.beat(MEMBER_ID, session.getId(), 900L))
                .isInstanceOf(InvalidGameSessionException.class)
                .hasMessageContaining("허용 범위를 초과");
    }

    @Test
    void 경과_시간에_비해_점수가_너무_낮으면_반려된다() {
        // 시간만 흘려보내고 점수는 거의 안 오른 상태 = 시간 부풀리기
        GameSession session = sessionStartedSecondsAgo(120);
        session.recordBeat(5L, LocalDateTime.now());
        given(session);

        assertThatThrownBy(() -> gameService.beat(MEMBER_ID, session.getId(), 10L))
                .isInstanceOf(InvalidGameSessionException.class)
                .hasMessageContaining("플레이 시간과 맞지 않습니다");
    }

    @Test
    void 점수가_감소하면_반려된다() {
        GameSession session = sessionStartedSecondsAgo(60);
        session.recordBeat(360L, LocalDateTime.now());
        given(session);

        assertThatThrownBy(() -> gameService.beat(MEMBER_ID, session.getId(), 100L))
                .isInstanceOf(InvalidGameSessionException.class)
                .hasMessageContaining("점수가 감소했습니다");
    }

    @Test
    void 정상_하트비트는_기록된다() {
        GameSession session = sessionStartedSecondsAgo(60);
        session.recordBeat(300L, LocalDateTime.now().minusSeconds(5));
        int before = session.getBeatCount();
        given(session);

        long score = (long) calculator.expectedScore(60);
        gameService.beat(MEMBER_ID, session.getId(), score);

        assertThat(session.getStatus()).isEqualTo(GameSessionStatus.ACTIVE);
        assertThat(session.getBeatCount()).isEqualTo(before + 1);
        assertThat(session.getLastScore()).isEqualTo(score);
    }

    // ── 동시 세션 제한 ──────────────────────────────────────────────

    @Test
    void 동시_활성_세션이_한도를_넘으면_오래된_것부터_무효화된다() {
        GameSession old1 = sessionStartedSecondsAgo(300);
        GameSession old2 = sessionStartedSecondsAgo(200);
        GameSession old3 = sessionStartedSecondsAgo(100);

        when(sessionRepository.findAliveSessions(eq(MEMBER_ID), eq(GameSessionStatus.ACTIVE), any()))
                .thenReturn(new java.util.ArrayList<>(java.util.List.of(old1, old2, old3)));
        when(memberRepository.getReferenceById(MEMBER_ID)).thenReturn(member);
        when(sessionRepository.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

        gameService.issue(MEMBER_ID);

        // 한도 3, 기존 3개 + 신규 1개 = 4개 → 가장 오래된 1개만 밀려난다
        assertThat(old1.getStatus()).isEqualTo(GameSessionStatus.INVALIDATED);
        assertThat(old2.getStatus()).isEqualTo(GameSessionStatus.ACTIVE);
        assertThat(old3.getStatus()).isEqualTo(GameSessionStatus.ACTIVE);
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────

    private GameSession sessionStartedSecondsAgo(long seconds) {
        GameSession session = new GameSession(member);
        ReflectionTestUtils.setField(session, "startedAt", LocalDateTime.now().minusSeconds(seconds));
        return session;
    }

    /** elapsed초 동안 정상적으로 플레이하며 하트비트를 보낸 상태로 만든다. */
    private void playedNormally(GameSession session, double elapsed, int beats) {
        ReflectionTestUtils.setField(session, "beatCount", beats);
        ReflectionTestUtils.setField(session, "lastScore", (long) calculator.expectedScore(elapsed));
        ReflectionTestUtils.setField(session, "lastBeatAt", LocalDateTime.now());
    }

    private void given(GameSession session) {
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
    }
}
