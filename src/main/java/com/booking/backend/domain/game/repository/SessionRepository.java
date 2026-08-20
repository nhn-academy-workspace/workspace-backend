package com.booking.backend.domain.game.repository;

import com.booking.backend.domain.game.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<GameSession, String> {
}
