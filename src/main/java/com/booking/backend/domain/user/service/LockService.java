package com.booking.backend.domain.user.service;

import com.booking.backend.domain.book.entity.Booking;
import com.booking.backend.domain.book.repository.BookingRepository;
import com.booking.backend.domain.notification.entity.NotiType;
import com.booking.backend.domain.notification.event.NotificationRequestedEvent;
import com.booking.backend.domain.room.Room;
import com.booking.backend.domain.room.RoomLock;
import com.booking.backend.domain.room.repository.RoomLockRepository;
import com.booking.backend.domain.room.repository.RoomRepository;
import com.booking.backend.domain.user.dto.LockRequest;
import com.booking.backend.domain.user.dto.LockResponse;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.exception.exception.InvalidLockTimeException;
import com.booking.backend.exception.exception.LockNotFoundException;
import com.booking.backend.exception.exception.MemberNotFoundException;
import com.booking.backend.exception.exception.RoomNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LockResponse create(LockRequest req, Long taId) {

        Room room = roomRepository.findById(req.roomId()).orElseThrow(
                () -> new RoomNotFoundException("해당 회의실이 존재하지 않습니다. : " + req.roomId())
        );

        Member ta = memberRepository.findById(taId).orElseThrow(
                () -> new MemberNotFoundException("해당 TA를 찾을 수 없습니다. : " + taId)
        );

        validateLockTime(req.startTime(), req.endTime());
        adjustConflictingBookings(req.roomId(), req.startTime(), req.endTime(), taId);

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

    @Transactional
    public LockResponse update(Long lockId, LockRequest req, Long taId) {

        RoomLock lock = roomLockRepository.findById(lockId).orElseThrow(
                () -> new LockNotFoundException("해당 Lock을 찾을 수 없습니다. : " + lockId)
        );

        validateLockTime(req.startTime(), req.endTime());
        adjustConflictingBookings(lock.getRoom().getId(), req.startTime(), req.endTime(), taId);

        lock.update(req.startTime(), req.endTime(), req.reason());

        return new LockResponse(lock.getId(), lock.getRoom().getId(), req.startTime(), req.endTime(), req.reason());
    }

    @Transactional
    public void remove(Long lockId) {
        if(!roomLockRepository.existsById(lockId)) {
            throw new LockNotFoundException("해당 Lock을 찾을 수 없습니다. : " + lockId);
        }
        roomLockRepository.deleteById(lockId);
    }

    // TA Lock은 과거 기록용으로 지난 기간도 허용 — 당일 제약 없음. 운영시간/단위/순서만 검증.
    private void validateLockTime(LocalDateTime start, LocalDateTime end) {

        if(!start.toLocalDate().isEqual(end.toLocalDate())) {
            throw new InvalidLockTimeException("Lock은 같은 날짜 내에서만 지정할 수 있습니다.");
        }

        if(!end.isAfter(start)) {
            throw new InvalidLockTimeException("종료 시간은 시작 시간보다 늦어야 합니다.");
        }

        // 5분 단위
        if(ChronoUnit.MINUTES.between(start, end) % 5 != 0) {
            throw new InvalidLockTimeException("Lock은 5분 단위로 할 수 있습니다.");
        }

        LocalTime minTime = LocalTime.of(9, 0);
        LocalTime maxTime = LocalTime.of(18, 0);

        if(start.toLocalTime().isBefore(minTime) || end.toLocalTime().isAfter(maxTime)) {
            throw new InvalidLockTimeException("잘못된 요청입니다. 09:00 ~ 18:00 이내에만 Lock을 할 수 있습니다.");
        }
    }

    private void adjustConflictingBookings(Long roomId, LocalDateTime lockStart, LocalDateTime lockEnd, Long taId) {

        List<Booking> bookingList = bookingRepository.findConflictBookingAt(roomId, lockStart, lockEnd);

        for (Booking b : bookingList) {
            LocalDateTime start = b.getStartTime();
            LocalDateTime end = b.getEndTime();

            if (start.isBefore(lockStart) && !end.isAfter(lockEnd)) { // 예약이 앞에 걸친 경우 → 뒤를 자름
                b.setAdjustedAt(start, lockStart, taId);
                eventPublisher.publishEvent(new NotificationRequestedEvent(b.getId(), NotiType.TIME_CHANGED));
            } else if (!start.isBefore(lockStart) && end.isAfter(lockEnd)) { // 예약이 뒤에 걸친 경우 → 앞을 자름
                b.setAdjustedAt(lockEnd, end, taId);
                eventPublisher.publishEvent(new NotificationRequestedEvent(b.getId(), NotiType.TIME_CHANGED));
            } else if (start.isBefore(lockStart) && end.isAfter(lockEnd)) { // Lock이 예약 가운데 → 앞부분만 남김
                b.setAdjustedAt(start, lockStart, taId);
                eventPublisher.publishEvent(new NotificationRequestedEvent(b.getId(), NotiType.TIME_CHANGED));
            } else { // Lock이 예약을 다 덮는 경우 → 취소
                b.cancelled();
                eventPublisher.publishEvent(new NotificationRequestedEvent(b.getId(), NotiType.CANCELLED));
            }
        }
    }
}
