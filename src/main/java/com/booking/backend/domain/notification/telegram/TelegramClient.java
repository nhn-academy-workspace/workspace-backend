package com.booking.backend.domain.notification.telegram;

import com.booking.backend.domain.notification.dto.SendMessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class TelegramClient {

    private final TelegramProperties properties;
    private final RestClient restClient;

    public void sendMessage(Long chatId, String text) {
        restClient.post()
                .uri("https://api.telegram.org/bot{token}/sendMessage", properties.botToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SendMessageRequest(chatId, text))
                .retrieve()
                .toBodilessEntity();
    }
}
