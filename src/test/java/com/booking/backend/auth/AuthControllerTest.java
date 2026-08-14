package com.booking.backend.auth;

import com.booking.backend.auth.dto.PasswordRequest;
import com.booking.backend.auth.dto.PasswordResponse;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.context.SecurityContextRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Test
    void changePassword_updatesSessionCachedPrincipal() {
        Member member = Member.builder()
                .id(1L)
                .loginId("student1")
                .name("테스트")
                .password("password123")
                .role(Role.STUDENT)
                .mustChangePassword(true)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(member);

        AuthService authService = mock(AuthService.class);
        when(authService.changePassword(any(), any())).thenReturn(new PasswordResponse(false));

        AuthController controller = new AuthController(
                mock(AuthenticationManager.class),
                mock(SecurityContextRepository.class),
                authService
        );

        controller.changePassword(userDetails, new PasswordRequest("password123", "newpassword"));

        assertThat(userDetails.getMember().isMustChangePassword()).isFalse();
    }
}
