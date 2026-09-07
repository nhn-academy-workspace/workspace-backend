package com.booking.backend.domain.notification.service;

import com.booking.backend.domain.notification.entity.Notification;
import com.booking.backend.domain.notification.entity.NotificationStatus;
import com.booking.backend.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// 텔레그램 미연동으로 실패한 알림은 재시도 스윕에서 제외되는지 확인
// (연동 전 밀린 알림이 연동 순간 한꺼번에 발송되던 버그의 회귀 테스트)
// retryFailed()가 NotificationService에서 ReminderSender로 옮겨가서 테스트 대상도 같이 옮김
class ReminderSenderRetryTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final NotificationSender notificationSender = mock(NotificationSender.class);

    // mock이 아니라 실제 인스턴스 — 진짜 필터링 로직을 태워야 하니까
    private final ReminderSender reminderSender = new ReminderSender(notificationRepository, notificationSender);

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

        reminderSender.retryFailed();

        ArgumentCaptor<Notification> sent = ArgumentCaptor.forClass(Notification.class);
        verify(notificationSender, times(1)).send(sent.capture());
        assertThat(sent.getValue()).isEqualTo(networkError);
    }

    // 회귀 테스트: 재시도 소진으로 한 건이 예외를 던져도 같은 사이클의 나머지 건은 계속 처리돼야 함
    // (retryFailed()가 단일 트랜잭션이라, 예외가 전파되면 이미 성공한 markSent()까지 롤백됐던 버그)
    @Test
    void 한_건의_최종실패가_같은_사이클의_다른_알림_발송을_막지_않는다() {
        Notification willFail = Notification.builder()
                .id(1L).status(NotificationStatus.FAILED).failureReason("일시적 네트워크 오류").retryCount(1).build();
        Notification willSucceed = Notification.builder()
                .id(2L).status(NotificationStatus.FAILED).failureReason("일시적 네트워크 오류").retryCount(1).build();
        when(notificationRepository.findByStatusIn(List.of(NotificationStatus.FAILED)))
                .thenReturn(List.of(willFail, willSucceed));
        doThrow(new RuntimeException("재시도 소진")).when(notificationSender).send(willFail);

        reminderSender.retryFailed(); // 예외 없이 정상 반환돼야 함 (전파되면 @Transactional 전체가 롤백됨)

        verify(notificationSender, times(1)).send(willFail);
        verify(notificationSender, times(1)).send(willSucceed);
    }
}
