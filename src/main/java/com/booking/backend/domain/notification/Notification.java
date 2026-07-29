package com.booking.backend.domain.notification;

import com.booking.backend.domain.book.Booking;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@NoArgsConstructor
@Getter
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Booking booking;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotiType notiType;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    private String message;
}

