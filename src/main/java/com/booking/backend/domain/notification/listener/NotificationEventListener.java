package com.booking.backend.domain.notification.listener;

import com.booking.backend.domain.book.entity.Booking;
import com.booking.backend.domain.book.repository.BookingRepository;
import com.booking.backend.domain.notification.event.CallRequestEvent;
import com.booking.backend.domain.notification.event.NotificationRequestedEvent;
import com.booking.backend.domain.notification.service.NotificationService;
import com.booking.backend.exception.exception.BookingNotFoundException;
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
// 이벤트 종류(CANCELLED/TIME_CHANGED/CALL)에 따라 NotificationService 메서드 호출
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final BookingRepository bookingRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationRequestedEvent event) {
        log.info("알림 요청 이벤트 수신: {}", event);
        Booking booking = bookingRepository.findByIdWithTeamAndRoom(event.bookingId()).orElseThrow(()->new BookingNotFoundException("존재하지 않는 예약 아이디: 알림 전송 실패"));
        notificationService.notifyBooking(booking, event.notiType());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(CallRequestEvent event) {
        log.debug("호출 이벤트 수신: {}", event);
        notificationService.notifyCall(event.targetType(), event.targetId(), event.callerId(), event.message());
    }
}
