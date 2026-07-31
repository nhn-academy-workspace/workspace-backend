package com.booking.backend.domain.book.repository;

import com.booking.backend.domain.book.entity.BookingMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingMemberRepository extends JpaRepository<BookingMember, Long> {
}
