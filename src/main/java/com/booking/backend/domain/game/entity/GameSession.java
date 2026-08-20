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

    private boolean used = false;

    public GameSession(Member member) {
        this.id = UUID.randomUUID().toString();
        this.member = member;
    }

    public void markUsed() {
        this.used = true;
    }
}

