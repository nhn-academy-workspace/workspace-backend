package com.booking.backend.domain.notification.listener;

import com.booking.backend.domain.notification.event.NotificationRequestedEvent;
import com.booking.backend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// AFTER_COMMIT: 트랜잭션이 커밋된 뒤에만 실행 — 아직 안 반영된 Booking 조회 실패나,
//               롤백된 예약에 대한 알림 발송을 막기 위함.
// @Async: 알림 처리(DB 조회 + HTTP 호출)가 원래 요청의 응답 시간에 영향을 주지 않게.
// 주의(셀프 인보케이션): 반드시 NotificationService를 "다른 빈"으로 호출해야 @Async가 실제로 동작함.
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationRequestedEvent event) {
        // 이벤트 종류(CANCELLED/TIME_CHANGED/CALL)에 따라 NotificationService의 해당 처리 메서드를 호출
        // TODO: CANCELLED/TIME_CHANGED/CALL 구현 시 로직 추가
        log.info("알림 요청 이벤트 수신: {}", event);
    }
}
