package com.booking.backend.auth;

import com.booking.backend.auth.dto.PasswordRequest;
import com.booking.backend.auth.dto.PasswordResponse;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.exception.exception.InvalidCurrentPasswordException;
import com.booking.backend.exception.exception.InvalidNewPasswordException;
import com.booking.backend.exception.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public PasswordResponse changePassword(Long memberId, PasswordRequest req) {

        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new MemberNotFoundException("해당 멤버를 찾을 수 없습니다.")
        );

        if(!passwordEncoder.matches(req.currentPassword(), member.getPassword())) {
            throw new InvalidCurrentPasswordException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 현재 비밀번호, 새로운 비밀번호 일치 여부
        if(Objects.equals(req.currentPassword(), req.newPassword())) {
            throw new InvalidNewPasswordException("현재 비밀번호와 다른 비밀번호를 입력하세요");
        }

        // 길이 검증
        if(req.newPassword().length() < 8) {
            throw new InvalidNewPasswordException("비밀번호는 8자리 이상이여야 합니다.");
        }

        member.setPassword(passwordEncoder.encode(req.newPassword()));
        member.setMustChangePassword(false);

        return new PasswordResponse(false);
    }
}
