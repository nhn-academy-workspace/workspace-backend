package com.booking.backend.domain.notification;

import com.booking.backend.domain.book.entity.Booking;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_dedup",
                columnNames = {"book_id", "type", "target_id"}
        )
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Booking booking; // 예약과 무관한 알림은 null

    @Enumerated(value = EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotiType notiType;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    private String message;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public static Notification createPending(Booking booking, NotiType notiType, TargetType targetType,
                                              Long targetId, NotificationChannel channel, String message) {
        return Notification.builder()
                .booking(booking)
                .notiType(notiType)
                .targetType(targetType)
                .targetId(targetId)
                .channel(channel)
                .status(NotificationStatus.PENDING)
                .message(message)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = reason;
    }

    public void increaseRetryCount() {
        this.retryCount++;
    }

    public void markRead() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }
}
