package com.booking.backend.domain.notification.service;

import com.booking.backend.exception.exception.MemberNotFoundException;
import com.booking.backend.exception.exception.NotValidBotUsernameException;
import com.booking.backend.domain.notification.telegram.TelegramClient;
import com.booking.backend.domain.notification.telegram.TelegramProperties;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// notification-design — 온보딩(딥링크+웹훅) 흐름의 도메인 로직.
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramLinkService {

    private final MemberRepository memberRepository;
    private final TelegramClient telegramClient;
    private final TelegramProperties properties;


    /**
     * @Transactional이 새 영속성 컨텍스트를 열긴 하지만, 파라미터로 member를 받으면 member객체가 영속성 컨텍스트에 편입되지 않아서
     * 토큰 필드를 변경해도 더티 체킹 대상이 아니라 update 쿼리가 발생하지 않기 떄문에 DB에 토큰이 저장되지 않음.
     */
    @Transactional
    public String startLink(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()->new MemberNotFoundException("존재하지 않는 회원입니다."));

        String token = UUID.randomUUID().toString();
        member.issueTelegramLinkToken(token);

        if(properties.botUsername() == null){
            throw new NotValidBotUsernameException("Bot Username이 null입니다.");
        }
        return "https://t.me/" + properties.botUsername() + "?start=" + token;
    }

    @Transactional
    public void completeLink(String token, Long chatId) {
        Member member = memberRepository.findByTelegramLinkToken(token).orElse(null);
        if(member == null){
            return;
        }
        member.linkChat(chatId);

        try {
            telegramClient.sendMessage(chatId, "텔레그램 알림 연동 완료");
        } catch (Exception e) {
            // 실패해도 무시해버려
        }
    }
}
