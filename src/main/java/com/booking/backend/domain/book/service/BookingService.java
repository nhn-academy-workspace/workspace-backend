package com.booking.backend.domain.book.service;

import com.booking.backend.domain.book.dto.*;
import com.booking.backend.domain.book.entity.BookStatus;
import com.booking.backend.domain.book.entity.Booking;
import com.booking.backend.domain.book.entity.BookingMember;
import com.booking.backend.domain.book.repository.BookingMemberRepository;
import com.booking.backend.domain.book.repository.BookingRepository;
import com.booking.backend.domain.notification.entity.NotiType;
import com.booking.backend.domain.notification.event.NotificationRequestedEvent;
import com.booking.backend.domain.room.Room;
import com.booking.backend.domain.room.repository.RoomLockRepository;
import com.booking.backend.domain.room.repository.RoomRepository;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.exception.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final MemberRepository memberRepository;
    private final RoomRepository roomRepository;
    private final BookingMemberRepository bookingMemberRepository;
    private final RoomLockRepository roomLockRepository;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    public BookingResponse create(Long memberId, BookingRequest req) {

        /**
         * 예약 조건
         * 1. 15분 단위여야 함
         * 2. 한 번에 최대 두시간이여야 함
         * 3. 팀당 하루 4시간이 최대
         * 4. ~~같은 팀은 연속 예약은 1시간 간격이 필요~~
         * 5. 해당 날짜 08:30 부터 예약 오픈
         * 6. 최소 4명 이상 회의에 참여해야함
         */

        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new MemberNotFoundException("해당 멤버가 존재하지 않습니다. : " + memberId)
        );

        LocalDateTime start = req.startTime();
        LocalDateTime end = req.endTime();

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        // 이 팀의 오늘 예약 명단
        List<Booking> todayBookingList = bookingRepository.findBookingsByTeamIdAndDate(member.getTeam().getId(),startOfDay, endOfDay);

        long todayDuration = todayBookingList.stream().mapToLong(Booking::getBookingDuration).sum();
        long totalDuration = todayDuration + ChronoUnit.MINUTES.between(req.startTime(), req.endTime());

        LocalTime nowTime = now.toLocalTime();
        LocalTime bookingStartTime = LocalTime.of(8, 30);

        // 예약하려는 날짜가 오늘 날짜가 아닌 경우
        if(!req.startTime().toLocalDate().isEqual(today) || !req.endTime().toLocalDate().isEqual(today)) {
            throw new InvalidBookingTimeException("잘못된 요청입니다. 오늘 날짜만 예약을 할 수 있습니다.");
        }

        // 15분 단위가 아닌 경우
        if(ChronoUnit.MINUTES.between(req.startTime().toLocalTime(), req.endTime().toLocalTime()) % 15 != 0) {
            throw new InvalidBookingTimeException("예약은 15분 단위로 할 수 있습니다.");
        }

        if(!nowTime.isAfter(bookingStartTime)) { // 08시 30분 이전에 예약하는 경우
            throw new OutsideBookingTimeException("08시 30분 부터 회의실을 예약할 수 있습니다.");
        }

        LocalTime minTime = LocalTime.of(9, 0);
        LocalTime maxTime = LocalTime.of(18, 0);

        // 예약 범위를 벗어난 경우
        if(req.startTime().toLocalTime().isBefore(minTime) ||
                req.endTime().toLocalTime().isAfter(maxTime) ||
                req.endTime().toLocalTime().isBefore(req.startTime().toLocalTime())) { // startTime보다 endTime이 먼저 있는 경우
            throw new InvalidBookingTimeException("잘못된 요청입니다. 09:00 ~ 18:00 이내에만 예약할 수 있습니다.");
        }

        if(ChronoUnit.MINUTES.between(start, end) > 120) { // 예약 시간이 두시간이 초과한 경우
            throw new BookingTimeExceedException("회의실은 한 번에 최대 2시간까지만 이용 가능합니다.");
        }

        if(totalDuration > 240) { // 해당 팀의 오늘 총 예약 시간이 4시간이 넘어가는 경우
            throw new BookingTimeLimitExceedException("하루에 최대 4시간을 초과하여 사용할 수 없습니다. 지금까지 사용 시간 : " + todayDuration/60 + "시간 " + todayDuration%60 + "분");
        }

        // 최소 인원 4명
        if(req.memberIds().stream().distinct().count() < 4) {
            throw new MinParticipantsNotMetException("최소 4명 이상이 회의에 참여하여야 합니다.");
        }

        List<Long> allMemberIds = memberRepository.findByTeamId(member.getTeam().getId()).stream()
                .map(Member::getId)
                .toList();

        if(!req.memberIds().contains(memberId)) {
            throw new InvalidBookingMemberException("신청자도 회의에 포함되어 있어야 합니다.");
        }

        // 팀에 소속되지 않은 ID가 있을때
        for(Long id : req.memberIds()) {
            if(!allMemberIds.contains(id)) {
                throw new InvalidBookingMemberException("팀 소속이 아닌 멤버가 소속되어 있습니다.");
            }
        }

        // -------- 해당 시간에 다른 예약 있는지 확인하는 로직 --------

        boolean isBookingConflict = bookingRepository.existsConflictBookingAt(req.roomId(), req.startTime(), req.endTime());
        boolean isLockConflict = roomLockRepository.existsConflictBookingAt(req.roomId(), req.startTime(), req.endTime());

        if(isBookingConflict || isLockConflict) { // 예약이든 Lock이든 하나라도 겹치는게 있다면?
            throw new BookingConflictException("해당 시간에는 예약할 수 없습니다.");
        }

        // --- 동일한 멤버가 동일한 시간에 또 다른 예약이 있는지 확인 ----
        if(bookingMemberRepository.existsByMemberConflict(req.memberIds(), req.startTime(), req.endTime())) {
            throw new BookingConflictException("해당 시간에 예약 구성원 중 중복된 예약이 존재합니다.");
        }

        // ---- 예약 등록 로직 ----
        Room room = roomRepository.findById(req.roomId()).orElseThrow(
                () -> new RoomNotFoundException("해당 회의실을 찾을 수 없습니다. : " + req.roomId())
        );

        Booking booking = Booking.builder()
                .team(member.getTeam())
                .room(room)
                .member(member)
                .startTime(req.startTime())
                .endTime(req.endTime())
                .bookStatus(BookStatus.BOOKED)
                .build();

        List<Member> members = memberRepository.findByIdIn(req.memberIds());

        List<BookingMember> bookingMemberList = members.stream()
                .map(m -> new BookingMember(booking, m))
                .toList();

        Long bookingId = bookingRepository.save(booking).getId();
        bookingMemberRepository.saveAll(bookingMemberList);

        log.debug("✅ 예약 완료 : {}", bookingId);

        return new BookingResponse(bookingId, room.getId(), booking.getStartTime(), booking.getEndTime(), booking.getBookStatus());
    }

    @Transactional
    public ExtendResponse extend(Long memberId, Long bookingId, ExtendRequest req) {

        /** 연장 정책의 가드
         * 1. 종료 15분 전부터 종료 전까지만 가능함
         * 2. 연장 최소 단위는 15분
         * 3. 연장 분 합산이 4시간을 넘길 수는 없음
         * 4. 연장 시간과 다음 예약이 겹치면 연장 불가
         * 5. 본인 소속팀의 예약만 연장 가능
         * 6. 연장 횟수의 제한은 없음
         */

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime extendEndTime = req.endTime();
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new MemberNotFoundException("해당 멤버를 찾을 수 없습니다. : " + memberId)
        );

        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new BookingNotFoundException("해당 예약을 찾을 수 없습니다. : " + bookingId)
        );

        Long extendDuration = ChronoUnit.MINUTES.between(booking.getEndTime(), extendEndTime);

        // 0. 본인 소속팀의 예약인지?
        if(!Objects.equals(member.getTeam().getId(), booking.getTeam().getId())) {
            throw new InvalidBookingMemberException("본인의 소속팀만 연장할 수 있습니다.");
        }

        // 1. 연장 가능 시간(종료 15분 전)에 연장했는지? + 'BOOKED' 상태인지?
        LocalDateTime endTime = booking.getEndTime();

        if (now.isBefore(endTime.minusMinutes(15)) || !now.isBefore(endTime) || booking.getBookStatus() != BookStatus.BOOKED) {
            throw new ExtendNotAllowedException("사용 종료 15분 전부터 연장할 수 있습니다.");
        }

        // 2. 연장 최소 시간에 부합하는지 + 15분 단위인지?

        if(extendDuration < 15 || extendDuration % 15 != 0) {
            throw new InvalidBookingTimeException("연장은 최소 15분부터, 15분 단위로 가능합니다.");
        }

        // 3. 연장 시간 포함 총 시간이 4시간 이하인지

        List<Booking> todayBookingList = bookingRepository.findBookingsByTeamIdAndDate(member.getTeam().getId(), startOfDay, endOfDay);
        Long todayDuration = todayBookingList.stream()
                .mapToLong(Booking::getBookingDuration)
                .sum();

        if(todayDuration + extendDuration > 240) {
            throw new BookingTimeLimitExceedException("하루에 최대 4시간을 초과하여 사용할 수 없습니다. 지금까지 사용 시간 : " + todayDuration/60 + "시간 " + todayDuration%60 + "분");
        }

        // 4. 연장 시간이 다른 시간과 겹치지는 않는지?
        // 오늘 회의실 예약 목록
        boolean isBookingConflict = bookingRepository.existsConflictBookingAt(booking.getRoom().getId(), endTime, extendEndTime);
        boolean isLockConflict = roomLockRepository.existsConflictBookingAt(booking.getRoom().getId(), endTime, extendEndTime);
        if(isBookingConflict || isLockConflict || extendEndTime.toLocalTime().isAfter(LocalTime.of(18, 0))) {
            throw new BookingConflictException("해당 시간에는 예약할 수 없습니다.");
        }

        List<Long> memberIds = bookingMemberRepository.findMemberIdByBookingId(bookingId);
        if(bookingMemberRepository.existsByMemberConflict(memberIds, booking.getEndTime(), req.endTime())) {
            throw new BookingConflictException("해당 시간에 예약 구성원 중 중복된 예약이 존재합니다.");
        }

        // ---- 실제 저장 로직 ----
        booking.setEndTime(extendEndTime);

        eventPublisher.publishEvent(new NotificationRequestedEvent(bookingId, NotiType.TIME_CHANGED));

        return new ExtendResponse(bookingId, booking.getRoom().getId(), booking.getStartTime(), booking.getEndTime(), booking.getBookStatus());
    }

    @Transactional
    public EarlyReturnResponse earlyReturn(Long memberId, Long bookingId) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new MemberNotFoundException("해당 멤버를 찾을 수 없습니다. : " + memberId)
        );

        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new BookingNotFoundException("해당 예약을 찾을 수 없습니다. " + bookingId)
        );

        // 해당 멤버가 맞는지 확인
        if(!Objects.equals(booking.getTeam().getId(), member.getTeam().getId())) {
            throw new InvalidBookingMemberException("본인의 소속팀만 조기 반납할 수 있습니다.");
        }

        // BOOKED 상태인지 확인
        if(booking.getBookStatus() != BookStatus.BOOKED) {
            throw new InvalidBookingRequestException("해당 예약은 조기 반납할 수 없습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        if(booking.getStartTime().isAfter(now) || booking.getEndTime().isBefore(now)) {
            throw new InvalidBookingTimeException("조기 반납은 예약 시간 이내에 해야합니다.");
        }


        booking.setEndTime(now);
        booking.setBookStatus(BookStatus.EARLY_RETURNED);

        return new EarlyReturnResponse(booking.getBookStatus());
    }

    @Transactional
    public CancelResponse cancel(Long memberId, Long bookingId) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new MemberNotFoundException("해당 멤버를 찾을 수 없습니다. : " + memberId)
        );

        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new BookingNotFoundException("해당 예약을 찾을 수 없습니다. " + bookingId)
        );

        // 해당 멤버가 맞는지 확인
        if(!Objects.equals(booking.getTeam().getId(), member.getTeam().getId())) {
            throw new InvalidBookingMemberException("본인의 소속팀만 취소할 수 있습니다.");
        }

        // BOOKED 상태인지 확인
        if(booking.getBookStatus() != BookStatus.BOOKED) {
            throw new InvalidBookingRequestException("해당 예약은 조기 반납할 수 없습니다.");
        }

        // 예약 시작 시간 이전에만 취소 가능
        LocalDateTime now = LocalDateTime.now();
        if(!now.isBefore(booking.getStartTime())) {
            throw new InvalidBookingTimeException("취소는 예약 시간 이전에 해야합니다.");
        }

        // --- 실제 취소 ---

        booking.setBookStatus(BookStatus.CANCELLED);
        eventPublisher.publishEvent(new NotificationRequestedEvent(bookingId, NotiType.CANCELLED));

        return new CancelResponse(BookStatus.CANCELLED);
    }


}
