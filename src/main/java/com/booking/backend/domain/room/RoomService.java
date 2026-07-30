package com.booking.backend.domain.room;

import com.booking.backend.domain.book.Booking;
import com.booking.backend.domain.book.BookingRepository;
import com.booking.backend.domain.room.dto.BookingTimetableResponse;
import com.booking.backend.domain.room.dto.RoomResponse;
import com.booking.backend.domain.room.repository.RoomLockRepository;
import com.booking.backend.domain.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final RoomLockRepository roomLockRepository;

    // 모든 회의실의 상태를 반환
    public List<RoomResponse> checkRoomStatus() {

        LocalDateTime now = LocalDateTime.now();

        // 그때마다 계산해서 넘겨줌
        return roomRepository.findAll().stream()
                .map(room -> {
                    RoomStatus status;
                    if(roomLockRepository.existsActiveLockAt(room.getId(), now)) {
                        status = RoomStatus.LOCK;
                    } else if(bookingRepository.existsActiveBookingAt(room.getId(), now)) {
                        status = RoomStatus.OCCUPIED;
                    } else {
                        status = RoomStatus.AVAILABLE;
                    }

                    return new RoomResponse(room.getId(), room.getName(), status);
                }).toList();
    }

    // 특정 날짜의 특정 회의실의 예약 정보를 모두 불러옴
    public List<BookingTimetableResponse> getBookings(Long roomId, LocalDate date) {

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<Booking> bookingList = bookingRepository.findBookingsByRoomIdAndDate(roomId, startOfDay, endOfDay);
        List<RoomLock> roomLockList = roomLockRepository.findRoomLocksByRoomIdAndDate(roomId, startOfDay, endOfDay);

        return Stream.concat(
                bookingList.stream().map(BookingTimetableResponse::new),
                roomLockList.stream().map(BookingTimetableResponse::new)
        ).toList();
    }



}
