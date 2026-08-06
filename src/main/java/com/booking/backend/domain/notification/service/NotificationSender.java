package com.booking.backend.domain.notification.service;

import com.booking.backend.domain.notification.entity.Notification;

public interface NotificationSender {

    void send(Notification notification);
}
