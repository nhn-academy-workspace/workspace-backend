package com.booking.backend.auth;

import com.booking.backend.auth.dto.PasswordRequest;
import com.booking.backend.auth.dto.PasswordResponse;
import com.booking.backend.domain.notification.telegram.TelegramClient;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.exception.exception.InvalidCurrentPasswordException;
import com.booking.backend.exception.exception.InvalidNewPasswordException;
import com.booking.backend.exception.exception.MemberNotFoundException;
import com.booking.backend.exception.exception.NotificationException;
import com.booking.backend.exception.exception.TelegramNotLinkedException;
import com.booking.backend.util.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TelegramClient telegramClient;

    @Transactional
    public PasswordResponse changePassword(Long memberId, PasswordRequest req) {

        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new MemberNotFoundException("해당 멤버를 찾을 수 없습니다.")
        );

        // 길이 검증
        if(req.newPassword().length() < 8) {
            throw new InvalidNewPasswordException("비밀번호는 8자리 이상이여야 합니다.");
        }

        if(!passwordEncoder.matches(req.currentPassword(), member.getPassword())) {
            throw new InvalidCurrentPasswordException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 현재 비밀번호, 새로운 비밀번호 일치 여부
        if(Objects.equals(req.currentPassword(), req.newPassword())) {
            throw new InvalidNewPasswordException("현재 비밀번호와 다른 비밀번호를 입력하세요");
        }



        member.setPassword(passwordEncoder.encode(req.newPassword()));
        member.setMustChangePassword(false);

        return new PasswordResponse(false);
    }

    @Transactional
    public void resetPassword(String loginId) {

        Member member = memberRepository.findByLoginId(loginId).orElseThrow(
                () -> new MemberNotFoundException("해당 멤버를 찾을 수 없습니다.")
        );

        if (!member.hasChatLinked()) {
            throw new TelegramNotLinkedException("텔레그램 연동 후 사용 가능합니다.");
        }

        String newPwd = PasswordGenerator.generate();
        String message = String.format("[%s] 임시 비밀번호 발급: %s", loginId, newPwd);

        try {
            telegramClient.sendMessage(member.getChatId(), message);
        } catch (RestClientException e) {
            log.warn("비밀번호 초기화 텔레그램 전송 실패: {}", loginId, e);
            throw new NotificationException("비밀번호 초기화 실패. 다시 시도해주세요.", HttpStatus.BAD_GATEWAY);
        }

        member.setPassword(passwordEncoder.encode(newPwd));
        member.setMustChangePassword(true);
        log.debug("비밀번호 초기화 완료 : {}", loginId);
    }

}
