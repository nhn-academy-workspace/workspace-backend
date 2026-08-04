package com.booking.backend.domain.notification.telegram;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramMessage(
        @JsonProperty("message_id") Long messageId,
        String text,
        TelegramChat chat
) {
}
