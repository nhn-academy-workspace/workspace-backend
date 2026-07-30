package com.booking.backend.domain.room;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rooms")
@NoArgsConstructor
@Getter
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

//    @Enumerated(EnumType.STRING)
//    @Column(name = "room_status", nullable = false)
//    private RoomStatus roomStatus;
//     ---> 저장해야 하는 값이 아니라 그때마다 계산해야 하는 값

}
