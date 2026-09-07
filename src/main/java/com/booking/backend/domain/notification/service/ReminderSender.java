package com.booking.backend.domain.notification.service;

import com.booking.backend.domain.book.entity.Booking;
import com.booking.backend.domain.notification.entity.*;
import com.booking.backend.domain.notification.repository.NotificationRepository;
import com.booking.backend.domain.user.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderSender {

    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;

    private static final int MAX_RETRY_COUNT = 3;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendOne(Booking booking, NotiType notiType, Member member, String message){
        boolean alreadyExists = notificationRepository
                .existsByBookingAndNotiTypeAndTargetTypeAndTargetId(booking, notiType, TargetType.MEMBER, member.getId());
        if(alreadyExists){
            return;
        }

        Notification notification = Notification
                .createPending(booking, notiType, TargetType.MEMBER, member.getId(), NotificationChannel.TELEGRAM, message);

        notificationRepository.saveAndFlush(notification);
        notificationSender.send(notification);

    }

    // NotificationScheduler의 재시도 스윕이 호출
    @Transactional
    public void retryFailed() {
        List<Notification> notificationList = notificationRepository.findByStatusIn(List.of(NotificationStatus.FAILED));

        notificationList.stream()
                .filter(notification -> notification.getRetryCount() < MAX_RETRY_COUNT)
                // 연동 전에 미리 보내졌던 알림들이 연동 후에 한번에 오지 않도록 NOT_LINKED_REASON 메시지 제외
                .filter(notification -> !TelegramNotificationSender.NOT_LINKED_REASON.equals(notification.getFailureReason()))
                // 한 건의 최종 실패(재시도 소진 시 예외 전파)가 같은 트랜잭션의 다른 성공 건까지 롤백시키지 않도록 격리
                .forEach(notification -> {
                    try {
                        notificationSender.send(notification);
                    } catch (Exception e) {
                        log.error("재시도 발송 실패 | notificationId={}", notification.getId(), e);
                    }
                });
    }
}
