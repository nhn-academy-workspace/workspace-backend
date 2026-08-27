package com.booking.backend.domain.game.entity;

import com.booking.backend.domain.user.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "game_sessions")
@NoArgsConstructor
@Getter
public class GameSession {

    // UUID 임
    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @CreatedDate
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameSessionStatus status = GameSessionStatus.ACTIVE;

    /** 마지막 하트비트를 수신한 서버 시각. 끊김 감지에 사용. */
    @Column(name = "last_beat_at")
    private LocalDateTime lastBeatAt;

    /** 누적 하트비트 수. 제출 시 최소 개수 검증에 사용. */
    @Column(name = "beat_count", nullable = false)
    private int beatCount = 0;

    /** 마지막 하트비트가 보고한 점수. 단조성/최종 점수 일치 검증에 사용. */
    @Column(name = "last_score", nullable = false)
    private long lastScore = 0L;

    public GameSession(Member member) {
        this.id = UUID.randomUUID().toString();
        this.member = member;
    }

    public boolean isActive() {
        return status == GameSessionStatus.ACTIVE;
    }

    /** 하트비트 수신 기록. 시각은 서버가 찍는다 — 클라이언트가 소급할 수 없다. */
    public void recordBeat(long score, LocalDateTime at) {
        this.lastScore = score;
        this.lastBeatAt = at;
        this.beatCount++;
    }

    public void markSubmitted() {
        this.status = GameSessionStatus.SUBMITTED;
    }

    public void invalidate() {
        this.status = GameSessionStatus.INVALIDATED;
    }
}
