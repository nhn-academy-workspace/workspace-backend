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
import com.booking.backend.exception.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
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

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // 예약하려는 날짜가 오늘 날짜가 아닌 경우
        if(!req.startTime().toLocalDate().isEqual(today) || !req.endTime().toLocalDate().isEqual(today)) {
            throw new InvalidLockTimeException("잘못된 요청입니다. 오늘 날짜에만 Lock을 할 수 있습니다.");
        }

        // 15분 단위가 아닌 경우
        if((ChronoUnit.MINUTES.between(req.startTime().toLocalTime(), req.endTime().toLocalTime()) % 15 != 0) ||
                req.startTime().isEqual(req.endTime())) {
            throw new InvalidLockTimeException("Lock은 15분 단위로 할 수 있습니다.");
        }

        LocalTime minTime = LocalTime.of(9, 0);
        LocalTime maxTime = LocalTime.of(18, 0);

        // 예약 범위를 벗어난 경우
        if(req.startTime().toLocalTime().isBefore(minTime) ||
                req.endTime().toLocalTime().isAfter(maxTime) ||
                req.endTime().toLocalTime().isBefore(req.startTime().toLocalTime())) { // startTime보다 endTime이 먼저 있는 경우
            throw new InvalidLockTimeException("잘못된 요청입니다. 09:00 ~ 18:00 이내에만 Lock을 할 수 있습니다.");
        }



        for (Booking b : bookingList) {
            LocalDateTime start = b.getStartTime();
            LocalDateTime end = b.getEndTime();

            if (start.isBefore(req.startTime()) && (end.isBefore(req.endTime()) || end.isEqual(req.endTime()))) {  // 예약이 앞에 있는 경우
                // end 수정
                b.setAdjustedAt(start, req.startTime(), taId);
            } else if ((start.isAfter(req.startTime()) || start.isEqual(req.startTime())) && end.isAfter(req.endTime())) { // 예약이 뒤에 걸친 경우
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



        // TODO Lock 생성 알림



        return new LockResponse(lockId, req.roomId(), req.startTime(), req.endTime(), req.reason());
    }

    @Transactional
    public void remove(Long lockId) {
        if(!roomLockRepository.existsById(lockId)) {
            throw new LockNotFoundException("해당 Lock을 찾을 수 없습니다. : " + lockId);
        }
        roomLockRepository.deleteById(lockId);


        // TODO Lock 취소 알림
        // 필요하면 하셈


    }
}
