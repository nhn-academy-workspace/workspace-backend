package com.booking.backend.domain.book;

import com.booking.backend.domain.book.dto.BookingRequest;
import com.booking.backend.domain.book.dto.BookingResponse;
import com.booking.backend.domain.book.entity.BookStatus;
import com.booking.backend.domain.book.entity.Booking;
import com.booking.backend.domain.book.entity.BookingMember;
import com.booking.backend.domain.book.repository.BookingMemberRepository;
import com.booking.backend.domain.book.repository.BookingRepository;
import com.booking.backend.domain.room.Room;
import com.booking.backend.domain.room.repository.RoomLockRepository;
import com.booking.backend.domain.room.repository.RoomRepository;
import com.booking.backend.domain.user.Member;
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
public class BookingService {

    private final BookingRepository bookingRepository;
    private final MemberRepository memberRepository;
    private final RoomRepository roomRepository;
    private final BookingMemberRepository bookingMemberRepository;
    private final RoomLockRepository roomLockRepository;


    @Transactional
    public BookingResponse create(Long memberId, BookingRequest req) {

        /**
         * 예약 조건
         * 1. 15분 단위여야 함 ---> 이건 프론트에서 컷 하니까 가드 안해도 되지 않나..?
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
            throw new BookingTimeLimitExceedException("하루에 최대 4시간을 초과하여 사용할 수 없습니다. 지금까지 사용 시간 : " + totalDuration/60 + "시간 " + totalDuration%60 + "분");
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

}
