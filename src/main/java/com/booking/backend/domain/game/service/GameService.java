package com.booking.backend.domain.game.service;

import com.booking.backend.domain.game.config.GameSessionProperties;
import com.booking.backend.domain.game.dto.*;
import com.booking.backend.domain.game.entity.GameSession;
import com.booking.backend.domain.game.entity.GameSessionStatus;
import com.booking.backend.domain.game.entity.Score;
import com.booking.backend.domain.game.repository.PlayCountEntry;
import com.booking.backend.domain.game.repository.ScoreRepository;
import com.booking.backend.domain.game.repository.SessionRepository;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.exception.exception.InvalidGameSessionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final ScoreRepository scoreRepository;
    private final SessionRepository sessionRepository;
    private final MemberRepository memberRepository;
    private final GameScoreCalculator scoreCalculator;
    private final GameSessionProperties props;

    @Transactional
    public SessionResponse issue(Long memberId) {

        LocalDateTime now = LocalDateTime.now();

        // 대량 발급 후 방치했다가 한꺼번에 제출하는 농사를 막는다.
        List<GameSession> alive = sessionRepository.findAliveSessions(
                memberId, GameSessionStatus.ACTIVE, now.minus(props.ttl()));
        int excess = alive.size() - (props.maxActiveSessions() - 1);
        for (int i = 0; i < excess && i < alive.size(); i++) {
            alive.get(i).invalidate();
        }

        Member member = memberRepository.getReferenceById(memberId);
        GameSession session = new GameSession(member);
        String sessionId = sessionRepository.save(session).getId();

        return new SessionResponse(sessionId, props.beatInterval().toMillis());
    }

    /**
     * 플레이 중 하트비트. 서버가 수신 시각을 직접 기록하므로,
     * 클라이언트는 "실제로 그 시간 동안 살아있었다"는 사실을 소급 위조할 수 없다.
     */
    @Transactional
    public void beat(Long memberId, String sessionId, Long score) {

        LocalDateTime now = LocalDateTime.now();
        GameSession session = loadOwnedActiveSession(memberId, sessionId, now);

        // 끊겼다 나중에 재개하는 방식으로 시간을 벌 수 없게 한다.
        LocalDateTime since = session.getLastBeatAt() != null
                ? session.getLastBeatAt()
                : session.getStartedAt();
        if (Duration.between(since, now).compareTo(props.maxBeatGap()) > 0) {
            invalidate(session, "하트비트가 끊긴 세션입니다.");
        }

        if (score == null || score < 0) {
            invalidate(session, "점수가 올바르지 않습니다.");
        }
        if (score < session.getLastScore()) {
            invalidate(session, "점수가 감소했습니다.");
        }

        validateScoreBand(session, score, now);

        session.recordBeat(score, now);
    }

    @Transactional
    public void submit(Long memberId, ScoreRequest req) {

        LocalDateTime now = LocalDateTime.now();
        GameSession session = loadOwnedActiveSession(memberId, req.sessionId(), now);

        Long score = req.score();
        if (score == null || score < 0) {
            invalidate(session, "점수가 올바르지 않습니다.");
        }

        double elapsedSeconds = elapsedSeconds(session, now);

        // 하트비트를 실제로 보내며 플레이했는가.
        long expectedBeats = (long) (elapsedSeconds * 1000 / props.beatInterval().toMillis()) - 1;
        if (session.getBeatCount() < expectedBeats) {
            invalidate(session, "플레이 기록이 충분하지 않습니다.");
        }

        // 죽자마자 제출했는가 (마지막 하트비트 이후 오래 방치하지 않았는가).
        if (session.getLastBeatAt() != null
                && Duration.between(session.getLastBeatAt(), now).compareTo(props.maxBeatGap()) > 0) {
            invalidate(session, "하트비트가 끊긴 세션입니다.");
        }

        // 최종 점수가 마지막으로 보고한 점수와 이어지는가.
        double sinceLastBeat = session.getLastBeatAt() != null
                ? Duration.between(session.getLastBeatAt(), now).toMillis() / 1000.0
                : elapsedSeconds;
        double maxGain = scoreCalculator.expectedScore(elapsedSeconds)
                - scoreCalculator.expectedScore(Math.max(0, elapsedSeconds - sinceLastBeat));
        if (score < session.getLastScore() || score > session.getLastScore() + maxGain + 2) {
            invalidate(session, "최종 점수가 플레이 기록과 일치하지 않습니다.");
        }

        validateScoreBand(session, score, now);

        session.markSubmitted();
        scoreRepository.save(new Score(session.getMember(), score));
    }

    // ── 검증 헬퍼 ────────────────────────────────────────────────────

    private GameSession loadOwnedActiveSession(Long memberId, String sessionId, LocalDateTime now) {

        GameSession session = sessionRepository.findById(sessionId).orElseThrow(
                () -> new InvalidGameSessionException("유효하지 않은 게임 세션입니다.")
        );

        if (!Objects.equals(session.getMember().getId(), memberId)) {
            throw new InvalidGameSessionException("본인의 세션이 아닙니다.");
        }
        if (!session.isActive()) {
            throw new InvalidGameSessionException(
                    session.getStatus() == GameSessionStatus.SUBMITTED
                            ? "이미 사용된 세션입니다."
                            : "무효화된 세션입니다.");
        }
        // TTL: 방치형 치팅의 획득 상한을 고정한다.
        if (Duration.between(session.getStartedAt(), now).compareTo(props.ttl()) > 0) {
            invalidate(session, "만료된 세션입니다.");
        }
        return session;
    }

    /**
     * 점수가 경과 시간에 대해 물리적으로 가능한 범위 안에 있는지 확인한다.
     * 상한은 과다 점수를, 하한은 "세션만 열어두고 방치하는" 시간 부풀리기를 막는다.
     */
    private void validateScoreBand(GameSession session, long score, LocalDateTime now) {

        double elapsed = elapsedSeconds(session, now);

        if (score > scoreCalculator.upperBound(elapsed, props.scoreUpperTolerance())) {
            invalidate(session, "점수가 허용 범위를 초과했습니다.");
        }
        if (score < scoreCalculator.lowerBound(elapsed, props.scoreLowerTolerance())) {
            invalidate(session, "점수가 플레이 시간과 맞지 않습니다.");
        }
    }

    private double elapsedSeconds(GameSession session, LocalDateTime now) {
        return Duration.between(session.getStartedAt(), now).toMillis() / 1000.0;
    }

    /** 검증 실패한 세션은 폐기한다 — 같은 세션으로 재시도할 수 없다. */
    private void invalidate(GameSession session, String message) {
        session.invalidate();
        log.debug("게임 세션 무효화 [{}] {}", session.getId(), message);
        throw new InvalidGameSessionException(message);
    }

    // ── 랭킹 조회 ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BestScoreResponse> getBestRanking() {
        List<Score> scores = scoreRepository.findBestScore();

        return scores.stream()
                .map(BestScoreResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TodayScoreResponse> getTodayRanking() {

        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        List<Score> scores = scoreRepository.findTodayBestScore(start, end);

        return scores.stream()
                .map(TodayScoreResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlayCountResponse> getPlayRanking() {
       List<PlayCountEntry> entries = scoreRepository.findTopByPlayCount();

       return entries.stream()
               .map(e -> new PlayCountResponse(e.getMemberName(), e.getTeamName(), e.getPlayCount()))
               .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamRankingResponse> getTeamRanking() {

        // 멤버별 최고점 집계
        Map<Member, Long> bestPerMember = scoreRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Score::getMember,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparingLong(Score::getGameScore)),
                                opt -> opt.map(Score::getGameScore).orElse(0L)
                        )
                ));

        // 팀별 평균 → 정렬 → Top 5
        return bestPerMember.entrySet().stream()
                .filter(e -> e.getKey().getTeam() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getKey().getTeam().getName(),
                        Collectors.averagingLong(Map.Entry::getValue)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(e -> new TeamRankingResponse(
                        e.getKey(),
                        BigDecimal.valueOf(e.getValue()).setScale(1, RoundingMode.HALF_UP)
                ))
                .toList();
    }
}
