package com.booking.backend.domain.user.repository;

import com.booking.backend.domain.user.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
}
