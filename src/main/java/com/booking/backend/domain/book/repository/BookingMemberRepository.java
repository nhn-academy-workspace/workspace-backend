package com.booking.backend.domain.book.repository;

import com.booking.backend.domain.book.entity.BookingMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingMemberRepository extends JpaRepository<BookingMember, Long> {


    @Query("SELECT COUNT(bm) > 0 FROM BookingMember bm " +
            "WHERE bm.member.id IN :memberIds AND bm.booking.bookStatus = 'BOOKED' " +
            "AND bm.booking.startTime < :endTime AND bm.booking.endTime > :startTime ")
    boolean existsByMemberConflict(@Param("memberIds") List<Long> memberIds,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);
}
