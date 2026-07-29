package com.booking.backend.domain.room;

import com.booking.backend.domain.room.dto.BookingTimetableResponse;
import com.booking.backend.domain.room.dto.RoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<List<RoomResponse>> checkRooms() {

        List<RoomResponse> roomResponseList = roomService.checkRoomStatus();

        return ResponseEntity.ok(roomResponseList);
    }

    @GetMapping("{roomId}/bookings")
    public ResponseEntity<List<BookingTimetableResponse>> getBookings(@PathVariable Long roomId,
                                                                      @RequestParam(required = false) LocalDate date
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        List<BookingTimetableResponse> res = roomService.getBookings(roomId, targetDate);

        return ResponseEntity.ok(res);
    }

}
