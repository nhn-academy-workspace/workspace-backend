package com.booking.backend.domain.book.repository;

import com.booking.backend.domain.book.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.bookingMembers bm " +
            "JOIN FETCH bm.member " +
            "WHERE b.room.id = :roomId " +
            "AND b.startTime >= :startOfDay AND b.startTime < :endOfDay " +
            "AND b.bookStatus != 'CANCELLED'")
    List<Booking> findBookingsByRoomIdAndDate(@Param("roomId") Long roomId,
                                              @Param("startOfDay") LocalDateTime startOfDay,
                                              @Param("endOfDay") LocalDateTime endOfDay);

    // 지금 시간을 기준으로 예약이 가능한지 여부를 조사함
    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
            "WHERE b.room.id = :roomId AND b.bookStatus = 'BOOKED' " +
            "AND b.startTime <= :now AND b.endTime > :now")
    boolean existsActiveBookingAt(@Param("roomId") Long roomId,
                                  @Param("now") LocalDateTime now);

    // 겹치는 예약이 있는지? 있으면 True
    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
            "WHERE b.room.id = :roomId AND b.bookStatus = 'BOOKED' " +
            "AND (b.startTime < :endTime AND b.endTime > :startTime) ")
    boolean existsConflictBookingAt(@Param("roomId") Long roomId,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    // 겹치는 예약을 가져옴
    @Query("SELECT b FROM Booking b " +
            "WHERE b.room.id = :roomId AND b.bookStatus = 'BOOKED' " +
            "AND (b.startTime < :endTime AND b.endTime > :startTime) ")
    List<Booking> findConflictBookingAt(@Param("roomId") Long roomId,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    // 특정 팀의 해당 날짜 예약을 전부 불러옴
    @Query("SELECT b FROM Booking b " +
            "WHERE b.team.id = :teamId AND b.bookStatus = 'BOOKED' " +
            "AND b.startTime >= :startOfDay AND b.endTime < :endOfDay")
    List<Booking> findBookingsByTeamIdAndDate(@Param("teamId") Long teamId,
                                              @Param("startOfDay") LocalDateTime startOfDay,
                                              @Param("endOfDay") LocalDateTime endOfDay);

    // 가장 최근의 예약을 찾아옴
    @Query("SELECT b FROM Booking b " +
            "WHERE b.team.id = :teamId AND b.bookStatus = 'BOOKED' " +
            "AND b.startTime >= :startOfDay AND b.endTime < :endOfDay " +
            "ORDER BY b.endTime DESC LIMIT 1")
    Optional<Booking> findBookingByTeamIdAndDateAndRecent(@Param("teamId") Long teamId,
                                                          @Param("startOfDay") LocalDateTime startOfDay,
                                                          @Param("endOfDay") LocalDateTime endOfDay);


    @Query("SELECT b FROM Booking b " +
            "WHERE b.team.id = :teamId " +
            "ORDER BY b.startTime DESC")
    List<Booking> findByTeamId(@Param("teamId") Long teamId);

    // 알림 스케줄러(NotificationScheduler)의 시작/종료 5분 전 폴링용 — [from, to) 구간에 시작/종료하는 예약 조회
    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.team " +
            "JOIN FETCH b.room " +
            "WHERE b.bookStatus = 'BOOKED' " +
            "AND b.startTime >= :from AND b.startTime < :to")
    List<Booking> findBookingsStartingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.team " +
            "JOIN FETCH b.room " +
            "WHERE b.bookStatus = 'BOOKED' " +
            "AND b.endTime >= :from AND b.endTime < :to")
    List<Booking> findBookingsEndingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.team " +
            "JOIN FETCH b.room " +
            "WHERE b.id = :id")
    Optional<Booking> findByIdWithTeamAndRoom(@Param("id") Long id);
}

