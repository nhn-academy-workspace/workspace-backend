package com.booking.backend.auth;

import com.booking.backend.auth.dto.*;
import com.booking.backend.domain.user.entity.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {

        var token = new UsernamePasswordAuthenticationToken(req.loginId(), req.password());
        // TODO 인증 실패시 예외처리 하기
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(token);
        } catch(AuthenticationException e) {
            return ResponseEntity.status(401).body(new ErrorResponse(e.getMessage()));
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Member member = userDetails.getMember();

        String teamName = member.getTeam() == null ? null : member.getTeam().getName();

        return ResponseEntity.ok(new LoginResponse(member.getName(), member.getRole(), teamName, member.isMustChangePassword())); // 임시로 해놓은거임
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = userDetails.getMember();
        String teamName = member.getTeam() == null ? null : member.getTeam().getName();
        return ResponseEntity.ok(new LoginResponse(member.getName(), member.getRole(), teamName, member.isMustChangePassword()));
    }

    @PatchMapping("/password")
    public ResponseEntity<PasswordResponse> changePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                           @RequestBody PasswordRequest req) {

        PasswordResponse res = authService.changePassword(userDetails.getMember().getId(), req);

        userDetails.getMember().setMustChangePassword(res.mustChangePassword());

        return ResponseEntity.ok(res);
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest req) {

        authService.resetPassword(req.loginId());

        return ResponseEntity.ok().build();
    }

}
