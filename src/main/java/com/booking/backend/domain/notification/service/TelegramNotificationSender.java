package com.booking.backend.domain.notification.service;

import com.booking.backend.domain.notification.entity.Notification;
import com.booking.backend.domain.notification.telegram.TelegramClient;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.exception.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

// @Retryable이 붙은 메서드라 반드시 별도 빈이어야 함
// NotificationService가 이 빈을 주입받아 호출
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramNotificationSender implements NotificationSender {

    // 연동 전에 온 메시지들은 전송 실패가 아니므로 재시도 시 전송 대상에서 제외
    public static final String NOT_LINKED_REASON = "텔레그램 미연동";

    private final TelegramClient telegramClient;
    private final MemberRepository memberRepository;

    @Override
    @Retryable(retryFor = RestClientException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2))
    public void send(Notification notification) {
        Member member = memberRepository.findById(notification.getTargetId())
                .orElseThrow(() -> new MemberNotFoundException("해당 멤버가 존재하지 않습니다. : " + notification.getTargetId()));

        if (member.getChatId() == null) {
            notification.markFailed(NOT_LINKED_REASON);
            return;
        }

        try {
            telegramClient.sendMessage(member.getChatId(), notification.getMessage());
            notification.markSent();
        } catch (Exception e) {
            notification.increaseRetryCount();
            throw e;
        }
    }
}
