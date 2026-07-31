package com.booking.backend.domain.room.repository;

import com.booking.backend.domain.room.RoomLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RoomLockRepository extends JpaRepository<RoomLock, Long> {


    @Query("SELECT r FROM RoomLock r " +
            "WHERE r.room.id = :roomId " +
            "AND r.startTime >= :startOfDay AND r.startTime < :endOfDay")
    List<RoomLock> findRoomLocksByRoomIdAndDate(@Param("roomId") Long roomId,
                                                @Param("startOfDay") LocalDateTime startOfDay,
                                                @Param("endOfDay") LocalDateTime endOfDay);


    @Query("SELECT COUNT(r) > 0 FROM RoomLock r " +
            "WHERE r.room.id = :roomId AND r.startTime <= :now AND r.endTime > :now")
    boolean existsActiveLockAt(@Param("roomId") Long roomId, @Param("now") LocalDateTime now);


    // 겹치는 Lock이 있는지? 있으면 True
    @Query("SELECT COUNT(r) > 0 FROM RoomLock r " +
            "WHERE r.room.id = :roomId " +
            "AND (r.startTime < :endTime AND r.endTime > :startTime)")
    boolean existsConflictBookingAt(@Param("roomId") Long roomId,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);
}
