package com.booking.backend.auth;

import com.booking.backend.domain.user.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

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

        return ResponseEntity.ok(new LoginResponse(member.getName(), member.getRole(), teamName)); // 임시로 해놓은거임
    }

}
