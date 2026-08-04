package com.booking.backend.domain.notification.repository;

import com.booking.backend.domain.book.entity.Booking;
import com.booking.backend.domain.notification.entity.NotiType;
import com.booking.backend.domain.notification.entity.Notification;
import com.booking.backend.domain.notification.entity.NotificationStatus;
import com.booking.backend.domain.notification.entity.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(TargetType targetType, Long targetId);

    List<Notification> findByStatusIn(List<NotificationStatus> statuses);

    // 멱등성 1차 방어선(존재 여부 사전 체크) — 2차 방어선은 Notification의 UNIQUE(book_id, type, target_id) 제약
    boolean existsByBookingAndNotiTypeAndTargetTypeAndTargetId(Booking booking, NotiType notiType,
                                                                TargetType targetType, Long targetId);
}
