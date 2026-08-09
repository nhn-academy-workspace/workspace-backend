package com.booking.backend.domain.notification.controller;

import com.booking.backend.auth.CustomUserDetails;
import com.booking.backend.domain.notification.dto.NotificationPreferenceRequest;
import com.booking.backend.domain.notification.dto.TelegramLinkResponse;
import com.booking.backend.domain.notification.dto.TelegramLinkStatusResponse;
import com.booking.backend.domain.notification.service.NotificationService;
import com.booking.backend.domain.notification.service.TelegramLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberNotificationController {

    private final TelegramLinkService telegramLinkService;
    private final NotificationService notificationService;

    @GetMapping("/me/telegram-link")
    public ResponseEntity<TelegramLinkStatusResponse> getTelegramLinkStatus(@AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean linked = notificationService.isChatLinked(userDetails.getMember().getId());

        return ResponseEntity.ok(new TelegramLinkStatusResponse(linked));
    }

    @PostMapping("/me/telegram-link")
    public ResponseEntity<TelegramLinkResponse> startLink(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String deepLink = telegramLinkService.startLink(userDetails.getMember().getId());

        return ResponseEntity.ok(new TelegramLinkResponse(deepLink));
    }

    @PatchMapping("/{id}/notification-preference")
    public ResponseEntity<Void> updateNotificationPreference(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                               @PathVariable Long id,
                                                               @RequestBody NotificationPreferenceRequest req) {
        notificationService.updateNotificationPreference(userDetails.getMember(), id, req.enabled());

        return ResponseEntity.noContent().build();
    }
}
