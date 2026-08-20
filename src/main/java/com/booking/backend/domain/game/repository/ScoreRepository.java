package com.booking.backend.domain.game.repository;

import com.booking.backend.domain.game.entity.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {


    @Query("SELECT s FROM Score s " +
            "WHERE s.gameScore = (SELECT MAX(s2.gameScore) FROM Score s2 WHERE s2.member = s.member) " +
            "ORDER BY s.gameScore DESC " +
            "LIMIT 5")
    List<Score> findBestScore();

    @Query("SELECT s FROM Score s " +
            "WHERE s.gameScore = (SELECT MAX(s2.gameScore) FROM Score s2 WHERE s2.member = s.member AND s2.createdAt BETWEEN :start AND :end) " +
            "AND s.createdAt BETWEEN :start AND :end " +
            "ORDER BY s.gameScore DESC " +
            "LIMIT 5")
    List<Score> findTodayBestScore(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT s.member.name AS memberName, s.member.team.name AS teamName, COUNT(s) AS playCount " +
            "FROM Score s " +
            "GROUP BY s.member " +
            "ORDER BY playCount DESC " +
            "LIMIT 5")
    List<PlayCountEntry> findTopByPlayCount();



}
