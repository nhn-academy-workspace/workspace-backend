package com.booking.backend.domain.game.scheduler;

import com.booking.backend.domain.game.config.GameSessionProperties;
import com.booking.backend.domain.game.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 게임 시작마다 세션 행이 하나씩 쌓이므로 주기적으로 비워준다.
 * TTL을 넘긴 세션은 어차피 제출에 쓸 수 없으니 보관할 이유가 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameSessionCleanupScheduler {

    private final SessionRepository sessionRepository;
    private final GameSessionProperties props;

    @Scheduled(fixedRate = 600_000)
    @Transactional
    public void purgeExpiredSessions() {
        LocalDateTime threshold = LocalDateTime.now().minus(props.ttl());
        int deleted = sessionRepository.deleteExpiredBefore(threshold);

        if (deleted > 0) {
            log.debug("만료된 게임 세션 {}건 삭제", deleted);
        }
    }
}
