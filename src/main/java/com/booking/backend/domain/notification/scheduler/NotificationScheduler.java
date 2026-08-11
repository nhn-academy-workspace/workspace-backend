package com.booking.backend.domain.notification.scheduler;

import com.booking.backend.domain.book.entity.Booking;
import com.booking.backend.domain.book.repository.BookingRepository;
import com.booking.backend.domain.notification.entity.NotiType;
import com.booking.backend.domain.notification.service.NotificationService;
import com.booking.backend.domain.notification.service.ReminderSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;


/**
 두 가지 역할을 겸함: 시작/종료 5분 전 폴링 + 실패 재시도 스윕.
 폴링 창(window)이 왜 [now+5분, now+6분)인가: fixedRate=60_000(1분 간격)이라, 이번 폴링이
 [now+5, now+6)을 보면 다음 폴링(1분 뒤)은 자연히 [now+6, now+7)을 보게 됨 — 두 창이
 겹치지도, 비지도 않아서 "5분 전"인 예약이 정확히 한 번의 폴링에만 걸림.
 다만 스케줄러 지연/재시작 등으로 어떤 예약이 두 번 걸리는 경우가 생겨도, NotificationService
 안에서 UNIQUE(book_id, type, target_id) 제약이 중복 저장을 막아주므로(2차 방어선) 안전함.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationService notificationService;
    private final BookingRepository bookingRepository;
    private final ReminderSender reminderSender;

    @Scheduled(fixedRate = 60_000)
    public void pollStartReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> targets = bookingRepository.findBookingsStartingBetween(now.plusMinutes(5), now.plusMinutes(6));

        for (Booking booking : targets) {
            createRemindersSafely(booking, NotiType.START_REMINDER);
        }
    }

    @Scheduled(fixedRate = 60_000)
    public void pollEndReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> targets = bookingRepository.findBookingsEndingBetween(now.plusMinutes(5), now.plusMinutes(6));

        for (Booking booking : targets) {
            createRemindersSafely(booking, NotiType.END_REMINDER);
        }
    }

    // 예약 하나 처리 중 예외가 나도 같은 폴링 사이클의 나머지 예약은 계속 처리되도록 격리
    private void createRemindersSafely(Booking booking, NotiType notiType) {
        try {
            notificationService.notifyBooking(booking, notiType);
        } catch (Exception e) {
            log.error("알림 생성 실패 | bookingId={}, notiType={}", booking.getId(), notiType, e);
        }
    }

    // spring-retry의 @Retryable이 이미 실패한 뒤, 더 느슨한 주기로 다시 시도
    @Scheduled(fixedRate = 300_000)
    public void retryFailedSweep() {
        reminderSender.retryFailed();
    }
}
