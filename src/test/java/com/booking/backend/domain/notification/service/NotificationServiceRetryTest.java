package com.booking.backend.domain.notification.service;

import com.booking.backend.domain.book.repository.BookingMemberRepository;
import com.booking.backend.domain.notification.entity.Notification;
import com.booking.backend.domain.notification.entity.NotificationStatus;
import com.booking.backend.domain.notification.repository.NotificationRepository;
import com.booking.backend.domain.user.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// 텔레그램 미연동으로 실패한 알림은 재시도 스윕에서 제외되는지 확인
// (연동 전 밀린 알림이 연동 순간 한꺼번에 발송되던 버그의 회귀 테스트)
class NotificationServiceRetryTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final NotificationSender notificationSender = mock(NotificationSender.class);

    private final NotificationService notificationService = new NotificationService(
            notificationRepository,
            mock(MemberRepository.class),
            mock(BookingMemberRepository.class),
            notificationSender
    );

    @Test
    void 텔레그램_미연동으로_실패한_알림은_재시도하지_않는다() {
        Notification notLinked = Notification.builder()
                .status(NotificationStatus.FAILED)
                .failureReason(TelegramNotificationSender.NOT_LINKED_REASON)
                .retryCount(0)
                .build();
        Notification networkError = Notification.builder()
                .status(NotificationStatus.FAILED)
                .failureReason("일시적 네트워크 오류")
                .retryCount(1)
                .build();
        when(notificationRepository.findByStatusIn(List.of(NotificationStatus.FAILED)))
                .thenReturn(List.of(notLinked, networkError));

        notificationService.retryFailed();

        ArgumentCaptor<Notification> sent = ArgumentCaptor.forClass(Notification.class);
        verify(notificationSender, times(1)).send(sent.capture());
        assertThat(sent.getValue()).isEqualTo(networkError);
    }
}
