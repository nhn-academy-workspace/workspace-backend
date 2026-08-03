package com.booking.backend.domain.book.controller;

import com.booking.backend.auth.CustomUserDetails;
import com.booking.backend.domain.book.service.BookingService;
import com.booking.backend.domain.book.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('STUDENT')") // TA의 접근 권한을 Security 차원에서 막음
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                         @RequestBody BookingRequest req) {


        BookingResponse res = bookingService.create(userDetails.getMember().getId(), req);

        return ResponseEntity.status(201).body(res);
    }

    // 연장
    @PatchMapping("/{bookingId}/extend")
    public ResponseEntity<ExtendResponse> extendBooking(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                        @PathVariable Long bookingId,
                                                        @RequestBody ExtendRequest req) {

        ExtendResponse res = bookingService.extend(userDetails.getMember().getId(), bookingId, req);

        return ResponseEntity.ok(res);
    }

    // 조기 반납
    @PatchMapping("/{bookingId}/early-return")
    public ResponseEntity<EarlyReturnResponse> earlyReturnBooking(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                  @PathVariable Long bookingId) {
        EarlyReturnResponse res = bookingService.earlyReturn(userDetails.getMember().getId(), bookingId);

        return ResponseEntity.ok(res);
    }
}
