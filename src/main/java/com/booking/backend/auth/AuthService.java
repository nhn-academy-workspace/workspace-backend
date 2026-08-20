package com.booking.backend.auth;

import com.booking.backend.auth.dto.PasswordRequest;
import com.booking.backend.auth.dto.PasswordResponse;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.exception.exception.InvalidCurrentPasswordException;
import com.booking.backend.exception.exception.InvalidNewPasswordException;
import com.booking.backend.exception.exception.MemberNotFoundException;
import com.booking.backend.exception.exception.TelegramNotLinkedException;
import com.booking.backend.util.PasswordGenerator;
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

        // 텔레그램 연동 여부 확인
        if (member.getChatId() == null) {
            throw new TelegramNotLinkedException("텔레그램 연동 후 사용 가능합니다.");
        }

        String newPwd = PasswordGenerator.generate();

        member.setPassword(passwordEncoder.encode(newPwd));
        log.debug("비밀번호 변경 완료 : {}", loginId);

        /** TODO 초기화된 비밀번호 텔레그램으로 전송
         * newPwd 보내면 됨
         * 근데 만약 전송 실패시 예외 던져서 트랜잭션이 롤백되도록 해야함
         * 안그러면 전송 실패했는데 비밀번호 바뀌어버려서 로그인 못함
         */


        member.setMustChangePassword(true);
        // --> 비밀번호 바로 바꾸라고 강제
    }

}
