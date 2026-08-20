package com.booking.backend.domain.game.controller;

import com.booking.backend.auth.CustomUserDetails;
import com.booking.backend.domain.game.dto.*;
import com.booking.backend.domain.game.entity.GameSession;
import com.booking.backend.domain.game.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/game")
@Slf4j
public class GameController {

    private final GameService gameService;

    // 게임 세션 발급
    @PostMapping("/sessions")
    public ResponseEntity<SessionResponse> issueSession(@AuthenticationPrincipal CustomUserDetails userDetails) {

        SessionResponse res = gameService.issue(userDetails.getMember().getId());

        return ResponseEntity.status(201).body(res);
    }

    // 게임 점수 제출
    @PostMapping("/scores")
    public ResponseEntity<Void> submitScore(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            @RequestBody ScoreRequest req) {

        gameService.submit(userDetails.getMember().getId(), req);

        return ResponseEntity.status(201).build();
    }

    // 역대 개인점수 랭킹 top5
    @GetMapping("/scores/top")
    public ResponseEntity<List<BestScoreResponse>> getBestRanking() {

        List<BestScoreResponse> res = gameService.getBestRanking();

        return ResponseEntity.ok(res);
    }

    // 오늘 랭킹
    @GetMapping("/scores/today")
    public ResponseEntity<List<TodayScoreResponse>> getTodayRanking() {

        List<TodayScoreResponse> res = gameService.getTodayRanking();

        return ResponseEntity.ok(res);
    }

    @GetMapping("/play-count")
    public ResponseEntity<List<PlayCountResponse>> getPlayRanking() {

        List<PlayCountResponse> res = gameService.getPlayRanking();

        return ResponseEntity.ok(res);
    }

    @GetMapping("/scores/teams")
    public ResponseEntity<List<TeamRankingResponse>> getTeamRanking() {

        List<TeamRankingResponse> res = gameService.getTeamRanking();

        return ResponseEntity.ok(res);
    }




}
