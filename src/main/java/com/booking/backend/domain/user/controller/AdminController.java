package com.booking.backend.domain.user.controller;

import com.booking.backend.auth.CustomUserDetails;
import com.booking.backend.domain.book.dto.BookingResponse;
import com.booking.backend.domain.user.dto.*;
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
    public ResponseEntity<List<TeamResponse>> getAllTeams() {

        List<TeamResponse> res = adminService.getAll();

        return ResponseEntity.ok(res);
    }

    @GetMapping("/members")
    public ResponseEntity<List<MemberResponse>> getAllMembers() {

        List<MemberResponse> res = adminService.getAllMembers();

        return ResponseEntity.ok(res);
    }

    @PatchMapping("/members/{memberId}")
    public ResponseEntity<MemberResponse> changeTeam(@PathVariable Long memberId,
                                           @RequestBody TeamChangeRequest req) {

        MemberResponse res = adminService.changeTeam(memberId, req.teamId());

        return ResponseEntity.ok(res);
    }

    // --- 예약 관련 ---
    @PatchMapping("/bookings/{bookingId}/adjust")
    public ResponseEntity<BookingResponse> adjustBooking(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                          @PathVariable Long bookingId,
                                                          @RequestBody AdjustRequest req) {

        BookingResponse res = adminService.adjustBooking(bookingId, req.startTime(), req.endTime(), userDetails.getMember().getId());

        return ResponseEntity.ok(res);
    }

    @PatchMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long bookingId) {

        BookingResponse res = adminService.cancelBooking(bookingId);

        return ResponseEntity.ok(res);
    }

    // 호출
    @PostMapping("/calls")
    public ResponseEntity<Void> callMember(@AuthenticationPrincipal CustomUserDetails userDetails,
                                           @RequestBody CallRequest req) {

        adminService.call(req, userDetails.getMember().getId());

        return ResponseEntity.ok().build();
    }

    // --- Lock 관련 ---
    @PostMapping("/room-locks")
    public ResponseEntity<LockResponse> createLock(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                   @RequestBody LockRequest req) {

        LockResponse res = lockService.create(req, userDetails.getMember().getId());

        return ResponseEntity.status(201).body(res);
    }

    @DeleteMapping("/room-locks/{lockId}")
    public ResponseEntity<Void> removeLock(@PathVariable Long lockId) {
        lockService.remove(lockId);
        return ResponseEntity.status(204).build();
    }


    // TA에 의한 비밀번호 초기화
    @PostMapping("/members/{memberId}/password/reset")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@PathVariable Long memberId) {

        ResetPasswordResponse res = adminService.resetPassword(memberId);

        return ResponseEntity.ok(res);
    }

}
