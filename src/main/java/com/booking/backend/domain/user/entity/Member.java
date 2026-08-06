package com.booking.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

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

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id") // TA는 NULL임
    private Team team;

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String name;

    @Setter
    @Column(nullable = false)
    private String password;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Setter
    @ColumnDefault("false")
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "telegram_link_token", unique = true)
    private String telegramLinkToken;

    @Column(name = "chat_id")
    private Long chatId; // 텔레그램 연동 전까지 null

    @Builder.Default
    @ColumnDefault("true")
    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled = true;

    public void issueTelegramLinkToken(String token) {
        this.telegramLinkToken = token;
    }

    public void linkChat(Long chatId) {
        this.chatId = chatId;
        this.telegramLinkToken = null; // 1회용 토큰이므로 연동 완료 시 초기화
    }

    public void updateNotificationPreference(boolean enabled) {
        this.notificationEnabled = enabled;
    }

    public boolean hasChatLinked() {
        return chatId != null;
    }
}
