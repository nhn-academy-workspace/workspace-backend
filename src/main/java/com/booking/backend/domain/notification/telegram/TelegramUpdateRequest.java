package com.booking.backend.domain.notification.telegram;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramUpdateRequest(
        @JsonProperty("update_id") Long updateId,
        TelegramMessage message
) {
}
