package com.booking.backend.domain.notification.scheduler;

import com.booking.backend.domain.book.entity.BookStatus;
import com.booking.backend.domain.book.entity.Booking;
import com.booking.backend.domain.book.entity.BookingMember;
import com.booking.backend.domain.book.repository.BookingMemberRepository;
import com.booking.backend.domain.book.repository.BookingRepository;
import com.booking.backend.domain.notification.entity.Notification;
import com.booking.backend.domain.notification.entity.NotificationStatus;
import com.booking.backend.domain.notification.repository.NotificationRepository;
import com.booking.backend.domain.notification.telegram.TelegramClient;
import com.booking.backend.domain.room.Room;
import com.booking.backend.domain.room.repository.RoomRepository;
import com.booking.backend.domain.user.entity.Member;
import com.booking.backend.domain.user.repository.MemberRepository;
import com.booking.backend.domain.user.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JOIN FETCH(b.team/b.room) + ReminderSender(REQUIRES_NEW) 분리 이후 회귀 테스트.
 * 한 폴링 사이클 안에 예약 A(정상 발송)와 예약 B(참여자 하나가 텔레그램 API 최종 실패)를 같이 둬도,
 * A는 B와 무관하게 정상적으로 커밋되는지(=더 이상 폴링 사이클 전체가 한 트랜잭션으로 묶이지 않는지) 확인.
 */
@SpringBootTest
class NotificationSchedulerIntegrationTest {

    @Autowired
    private NotificationScheduler scheduler;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private BookingMemberRepository bookingMemberRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private TeamRepository teamRepository;

    @MockBean
    private TelegramClient telegramClient;

    @Test
    void 한_폴링_사이클에서_한_예약의_최종실패가_다른_정상_예약을_롤백시키지_않는다() {
        Member okStudent = memberRepository.findById(2L).orElseThrow();
        okStudent.linkChat(999L);
        memberRepository.save(okStudent);

        Member failingStudent = memberRepository.findById(6L).orElseThrow(); // 2팀 소속(1팀과 무관한 예약으로 분리)
        failingStudent.linkChat(222L);
        memberRepository.save(failingStudent);

        doNothing().when(telegramClient).sendMessage(eq(999L), anyString()); // 예약 A: 정상 발송
        doThrow(new ResourceAccessException("텔레그램 API 응답 없음"))
                .when(telegramClient).sendMessage(eq(222L), anyString()); // 예약 B: 재시도 3번 다 실패

        LocalDateTime start = LocalDateTime.now().plusMinutes(5).plusSeconds(30);
        Room room1 = roomRepository.findById(1L).orElseThrow();
        Room room2 = roomRepository.findById(2L).orElseThrow();

        Booking bookingA = bookingRepository.save(Booking.builder()
                .team(teamRepository.findById(1L).orElseThrow())
                .room(room1).member(okStudent)
                .startTime(start).endTime(start.plusMinutes(60))
                .bookStatus(BookStatus.BOOKED).build());
        bookingMemberRepository.save(new BookingMember(bookingA, okStudent));

        Booking bookingB = bookingRepository.save(Booking.builder()
                .team(teamRepository.findById(2L).orElseThrow())
                .room(room2).member(failingStudent)
                .startTime(start).endTime(start.plusMinutes(60))
                .bookStatus(BookStatus.BOOKED).build());
        bookingMemberRepository.save(new BookingMember(bookingB, failingStudent));

        scheduler.pollStartReminders(); // 두 예약이 같은 폴링 사이클, 같은 트랜잭션 안에서 처리됨

        List<Notification> forBookingA = notificationRepository.findAll().stream()
                .filter(n -> n.getBooking() != null && bookingA.getId().equals(n.getBooking().getId()))
                .toList();

        // 예약 A: 참여자(okStudent, 연동됨) + 팀 담당 TA(1팀 ta_id=1, 이 테스트에서 미연동) = 2건
        assertThat(forBookingA).hasSize(2);

        Notification okStudentResult = forBookingA.stream()
                .filter(n -> n.getTargetId().equals(okStudent.getId())).findFirst().orElseThrow();
        assertThat(okStudentResult.getStatus()).isEqualTo(NotificationStatus.SENT);

        verify(telegramClient, times(1)).sendMessage(eq(999L), anyString());
    }
}
