package com.booking.backend.domain.game.service;

import com.booking.backend.domain.game.dto.*;
import com.booking.backend.domain.game.entity.GameSession;
import com.booking.backend.domain.game.entity.Score;
import com.booking.backend.domain.game.repository.PlayCountEntry;
import com.booking.backend.domain.game.repository.ScoreRepository;
import com.booking.backend.domain.game.repository.SessionRepository;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.exception.exception.InvalidGameSessionException;
import com.booking.backend.exception.exception.MemberNotFoundException;
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

    @Transactional
    public SessionResponse issue(Long memberId) {

        Member member = memberRepository.getReferenceById(memberId);

        GameSession session = new GameSession(member);

        String sessionId = sessionRepository.save(session).getId();

        return new SessionResponse(sessionId);
    }

    @Transactional
    public void submit(Long memberId, ScoreRequest req) {

        // 1. 세션 아이디가 DB에 존재하는가?
        GameSession session = sessionRepository.findById(req.sessionId()).orElseThrow(
                () -> new InvalidGameSessionException("유효하지 않은 게임 세션입니다.")
        );

        // 2. 세션 아이디의 오너가 지금 로그인 유저인가?
        if(!Objects.equals(session.getMember().getId(), memberId)) {
            throw new InvalidGameSessionException("본인의 세션이 아닙니다.");
        }

        // 3. used = false인가?
        if(session.isUsed()) {
            throw new InvalidGameSessionException("이미 사용된 세션입니다.");
        }

        // 4. score ≤ (now - startedAt).seconds × 9  (물리 상한, ~8~9점/초)
        validateScore(session.getStartedAt(), req.score());


        session.markUsed();
        scoreRepository.save(new Score(session.getMember(), req.score()));
    }

    private void validateScore(LocalDateTime startedAt, Long score) {
        long elapsedSeconds = Duration.between(startedAt, LocalDateTime.now()).getSeconds();
        long maxAllowed = elapsedSeconds * 9;
        if (score > maxAllowed) {
            throw new InvalidGameSessionException("점수가 허용 범위를 초과했습니다.");
        }
    }

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
