package com.booking.backend.domain.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SendMessageRequest(
        @JsonProperty("chat_id") Long chatId,
        String text){
}