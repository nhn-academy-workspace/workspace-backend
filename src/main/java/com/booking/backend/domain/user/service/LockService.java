package com.booking.backend.domain.user.service;

import com.booking.backend.domain.book.entity.Booking;
import com.booking.backend.domain.book.repository.BookingRepository;
import com.booking.backend.domain.room.Room;
import com.booking.backend.domain.room.RoomLock;
import com.booking.backend.domain.room.repository.RoomLockRepository;
import com.booking.backend.domain.room.repository.RoomRepository;
import com.booking.backend.domain.user.dto.LockRequest;
import com.booking.backend.domain.user.dto.LockResponse;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.exception.exception.MemberNotFoundException;
import com.booking.backend.exception.exception.RoomNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LockService {

    private final RoomLockRepository roomLockRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public LockResponse create(LockRequest req, Long taId) {

        List<Booking> bookingList = bookingRepository.findConflictBookingAt(req.roomId(), req.startTime(), req.endTime());
        Room room = roomRepository.findById(req.roomId()).orElseThrow(
                () -> new RoomNotFoundException("해당 회의실이 존재하지 않습니다. : " + req.roomId())
        );

        Member ta = memberRepository.findById(taId).orElseThrow(
                () -> new MemberNotFoundException("해당 TA를 찾을 수 없습니다. : " + taId)
        );

        for (Booking b : bookingList) {
            LocalDateTime start = b.getStartTime();
            LocalDateTime end = b.getEndTime();

            if (start.isBefore(req.startTime()) && end.isBefore(req.endTime())) {  // 예약이 앞에 있는 경우
                // end 수정
                b.setAdjustedAt(start, req.startTime(), taId);
            } else if (start.isAfter(req.startTime()) && end.isAfter(req.endTime())) { // 예약이 뒤에 걸친 경우
                b.setAdjustedAt(req.endTime(), end, taId);
                // start 수정
            } else if (start.isBefore(req.startTime()) && end.isAfter(req.endTime())) { // 예약을 가운데 걸친 경우
                b.setAdjustedAt(start, req.startTime(), taId);
            } else { // 그외 경우, 다 덮는 경우
                // 아예 취소
                b.cancelled();
            }
        }

        RoomLock lock = RoomLock.builder()
                .room(room)
                .member(ta)
                .startTime(req.startTime())
                .endTime(req.endTime())
                .reason(req.reason())
                .build();

        Long lockId = roomLockRepository.save(lock).getId();

        return new LockResponse(lockId, req.roomId(), req.startTime(), req.endTime(), req.reason());
    }
}
