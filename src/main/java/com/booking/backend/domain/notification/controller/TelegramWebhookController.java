package com.booking.backend.domain.notification.controller;

import com.booking.backend.exception.exception.NotValidSecretTokenException;
import com.booking.backend.domain.notification.service.TelegramLinkService;
import com.booking.backend.domain.notification.telegram.TelegramProperties;
import com.booking.backend.domain.notification.telegram.TelegramUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 텔레그램 서버가 호출하는 수신 전용 엔드포인트.
// SecurityConfig에 permitAll 등록 필요(인증 없음, 대신 secret-token 헤더로 검증).
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/telegram")
public class TelegramWebhookController {

    private static final String SECRET_TOKEN_HEADER = "X-Telegram-Bot-Api-Secret-Token";
    private static final String START_COMMAND_PREFIX = "/start ";

    private final TelegramLinkService telegramLinkService;
    private final TelegramProperties properties;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(SECRET_TOKEN_HEADER) String secretToken,
            @RequestBody TelegramUpdateRequest update) {

        if(!secretToken.equals(properties.webhookSecret())){
            throw new NotValidSecretTokenException("Webhook secret-token이 일치하지 않습니다.");
        }

        String text = update.message() != null ? update.message().text() : null;
        if (text != null && text.startsWith(START_COMMAND_PREFIX)) {
            String linkToken = text.substring(START_COMMAND_PREFIX.length()).trim();
            telegramLinkService.completeLink(linkToken, update.message().chat().id());
        }

        // 처리 결과와 무관하게 항상 200 — 텔레그램은 비-2xx를 실패로 보고 같은 업데이트를 재전송함
        return ResponseEntity.ok().build();
    }
}
