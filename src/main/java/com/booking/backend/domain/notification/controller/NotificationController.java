package com.booking.backend.domain.notification.controller;

import com.booking.backend.auth.CustomUserDetails;
import com.booking.backend.domain.notification.dto.NotificationResponse;
import com.booking.backend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 알림 이력 조회 2개 엔드포인트
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/me")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<NotificationResponse> res = notificationService.getMyNotifications(userDetails.getMember().getId());

        return ResponseEntity.ok(res);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal CustomUserDetails userDetails,
                                          @PathVariable Long id) {
        notificationService.markRead(userDetails.getMember().getId(), id);

        return ResponseEntity.noContent().build();
    }
}
