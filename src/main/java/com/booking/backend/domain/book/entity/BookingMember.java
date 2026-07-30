package com.booking.backend.domain.book.entity;

import com.booking.backend.domain.user.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="booking_members", uniqueConstraints = @UniqueConstraint(columnNames = {"book_id","member_id"})) // TODO 복합 유니크 관련해서 공부 좀 하기!!
@NoArgsConstructor
@Getter
public class BookingMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public BookingMember(Booking booking, Member member) {
        this.booking = booking;
        this.member = member;
    }
}
