package com.booking.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@NoArgsConstructor
@Getter
@AllArgsConstructor
@Builder
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id") // TA는 NULL임
    private Team team;

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "telegram_chat_id")
    private String telegramChatId; // 텔레그램 연동 전까지 null

    @Builder.Default
    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled = true;

    public void linkTelegram(String chatId) {
        this.telegramChatId = chatId;
    }

    public void updateNotificationPreference(boolean enabled) {
        this.notificationEnabled = enabled;
    }

    public boolean hasTelegramLinked() {
        return telegramChatId != null;
    }
}
