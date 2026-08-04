package com.booking.backend.domain.notification.service;

import com.booking.backend.domain.notification.exception.NotValidBotUsernameException;
import com.booking.backend.domain.notification.telegram.TelegramClient;
import com.booking.backend.domain.notification.telegram.TelegramProperties;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// notification-design.md #6 — 온보딩(딥링크+웹훅) 흐름의 도메인 로직.
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramLinkService {

    private final MemberRepository memberRepository;
    private final TelegramClient telegramClient;
    private final TelegramProperties properties;

    @Transactional
    public String startLink(Member member) {
        String token = UUID.randomUUID().toString();
        member.issueTelegramLinkToken(token);

        if(properties.botUsername() == null){
            throw new NotValidBotUsernameException("Bot Username이 null입니다.");
        }
        return "https://t.me/" + properties.botUsername() + "?start=" + token;
    }

    @Transactional
    public void completeLink(String token, Long chatId) {
        if(memberRepository.findByTelegramLinkToken(token).isEmpty()){
            return;
        }
        Member member = memberRepository.findByTelegramLinkToken(token).get();
        member.linkChat(chatId);

        try {
            telegramClient.sendMessage(chatId, "텔레그램 알림 연동 완료");
        } catch (Exception e) {
            // 실패해도 무시해버려
        }
    }
}
