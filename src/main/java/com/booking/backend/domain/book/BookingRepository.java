package com.booking.backend.domain.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b " +
            "WHERE b.room.id = :roomId " +
            "AND b.startTime >= :startOfDay AND b.startTime < :endOfDay " +
            "AND b.bookStatus != 'CANCELLED'")
    List<Booking> findBookingsByRoomIdAndDate(@Param("roomId") Long roomId,
                                              @Param("startOfDay") LocalDateTime startOfDay,
                                              @Param("endOfDay") LocalDateTime endOfDay);



    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
            "WHERE b.room.id = :roomId AND b.bookStatus = 'BOOKED' " +
            "AND b.startTime <= :now AND b.endTime > :now")
    boolean existsActiveBookingAt(@Param("roomId") Long roomId,
                                  @Param("now") LocalDateTime now);
}
