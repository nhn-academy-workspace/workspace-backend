package com.booking.backend.domain.book;

import com.booking.backend.auth.CustomUserDetails;
import com.booking.backend.domain.book.dto.BookingRequest;
import com.booking.backend.domain.book.dto.BookingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


}
