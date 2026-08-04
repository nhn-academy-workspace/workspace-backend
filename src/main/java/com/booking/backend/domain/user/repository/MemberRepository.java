package com.booking.backend.domain.user.repository;

import com.booking.backend.domain.user.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByLoginId(String loginId);

    List<Member> findByIdIn(List<Long> ids);

    @Query("SELECT m FROM Member m " +
            "WHERE m.team.id = :teamId")
    List<Member> findByTeamId(Long teamId);

    @Query("SELECT m FROM Member m " +
            "WHERE m.role = 'STUDENT' ")
    List<Member> findAllStudent();

    @Query("SELECT m FROM Member m " +
            "JOIN FETCH m.team WHERE m.role = 'STUDENT'")
    List<Member> findAllStudentWithTeam();
}

