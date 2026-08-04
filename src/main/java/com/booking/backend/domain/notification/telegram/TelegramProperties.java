package com.booking.backend.domain.notification.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "telegram")
public record TelegramProperties(
        String botToken,
        String botUsername,
        String webhookSecret)
{}
