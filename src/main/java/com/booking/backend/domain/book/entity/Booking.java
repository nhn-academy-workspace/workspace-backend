package com.booking.backend.domain.book.entity;

import com.booking.backend.domain.room.Room;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.entity.Team;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Entity
@Table(name = "bookings")
@NoArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@Builder
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

    @OneToMany(mappedBy = "booking")
    private List<BookingMember> bookingMembers;

    // 신청자 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Setter
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Setter
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

    // 예약 시간 반환
    public Long getBookingDuration() {
        return ChronoUnit.MINUTES.between(startTime, endTime);
    }

    public void setAdjustedAt(LocalDateTime startTime, LocalDateTime endTime, Long taId) {
        if (this.adjustedAt == null) {
            this.originalStartTime = this.startTime;
            this.originalEndTime = this.endTime;
        }

        this.startTime = startTime;
        this.endTime = endTime;
        this.adjustedById = taId;
        this.adjustedAt = LocalDateTime.now();
    }

    public void cancelled() {
        this.bookStatus = BookStatus.CANCELLED;
    }

    public List<String> getMemberNames() {
        return bookingMembers.stream().map(b -> b.getMember().getName()).toList();
    }
}
