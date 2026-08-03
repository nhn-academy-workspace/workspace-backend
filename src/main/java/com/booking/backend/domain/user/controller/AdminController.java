package com.booking.backend.domain.user.controller;

import com.booking.backend.auth.CustomUserDetails;
import com.booking.backend.domain.user.dto.AllMemberResponse;
import com.booking.backend.domain.user.dto.LockRequest;
import com.booking.backend.domain.user.dto.LockResponse;
import com.booking.backend.domain.user.service.AdminService;
import com.booking.backend.domain.user.service.LockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('TA')")
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;
    private final LockService lockService;


    @GetMapping("/teams")
    public ResponseEntity<List<AllMemberResponse>> getAllMember() {

        List<AllMemberResponse> res = adminService.getAll();

        return ResponseEntity.ok(res);
    }

    @PostMapping("/room-lock")
    public ResponseEntity<LockResponse> createLock(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                   @RequestBody LockRequest req) {

        LockResponse res = lockService.create(req, userDetails.getMember().getId());

        return ResponseEntity.status(201).body(res);
    }

    @DeleteMapping("/room-lock/{lockId}")
    public ResponseEntity<Void> removeLock(@PathVariable Long lockId) {
        lockService.remove(lockId);
        return ResponseEntity.status(204).build();
    }
}
