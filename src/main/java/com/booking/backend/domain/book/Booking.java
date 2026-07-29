package com.booking.backend.domain.book;

import com.booking.backend.domain.room.Room;
import com.booking.backend.domain.user.Member;
import com.booking.backend.domain.user.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@NoArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // 신청자 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "booking_status", nullable = false)
    private BookStatus bookStatus;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "original_start_time")
    private LocalDateTime originalStartTime;

    @Column(name = "original_end_time")
    private LocalDateTime originalEndTime;

    @Column(name = "adjusted_at")
    private LocalDateTime adjustedAt;

    @Column(name = "adjusted_by_id")
    private Long adjustedById;
}
