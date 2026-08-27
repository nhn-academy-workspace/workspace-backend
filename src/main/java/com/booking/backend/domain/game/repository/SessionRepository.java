package com.booking.backend.domain.game.repository;

import com.booking.backend.domain.game.entity.GameSession;
import com.booking.backend.domain.game.entity.GameSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<GameSession, String> {

    /** 특정 사용자의 살아있는 세션(오래된 순). 동시 세션 수 제한에 사용. */
    @Query("SELECT s FROM GameSession s " +
            "WHERE s.member.id = :memberId " +
            "AND s.status = :status " +
            "AND s.startedAt >= :notBefore " +
            "ORDER BY s.startedAt ASC")
    List<GameSession> findAliveSessions(@Param("memberId") Long memberId,
                                        @Param("status") GameSessionStatus status,
                                        @Param("notBefore") LocalDateTime notBefore);

    /** TTL 초과 세션 일괄 삭제. 게임 시작마다 행이 쌓이므로 정리가 필수. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM GameSession s WHERE s.startedAt < :threshold")
    int deleteExpiredBefore(@Param("threshold") LocalDateTime threshold);
}
