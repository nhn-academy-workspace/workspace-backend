package com.booking.backend.auth;

import com.booking.backend.domain.user.Member;
import com.booking.backend.domain.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 실제로 꽂히는건 loginId 값이 들어옴

        // TODO Global exception handler 작성하기
        Member member = memberRepository.findByLoginId(username).orElseThrow(
                () -> new UsernameNotFoundException("해당 사용자를 찾을 수 없습니다. : " + username)
        );

        return new CustomUserDetails(member);
    }
}
