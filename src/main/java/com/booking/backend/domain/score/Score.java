package com.booking.backend.domain.score;

import com.booking.backend.domain.user.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "scores")
@NoArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Score {


    /**
     *  scores
     * ├── id          BIGINT PK AUTO_INCREMENT
     * ├── member_id   BIGINT FK → members(id)
     * ├── score       INT NOT NULL
     * └── achieved_at DATETIME NOT NULL
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private Integer score;

    @CreatedDate
    @Column(name = "achieved_at", nullable = false)
    private LocalDateTime achievedAt;

}
