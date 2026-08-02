package com.booking.backend.domain.notification.repository;

import com.booking.backend.domain.notification.Notification;
import com.booking.backend.domain.notification.NotificationStatus;
import com.booking.backend.domain.notification.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(TargetType targetType, Long targetId);

    List<Notification> findByStatusIn(List<NotificationStatus> statuses);
}
