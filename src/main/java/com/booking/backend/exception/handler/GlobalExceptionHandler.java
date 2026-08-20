package com.booking.backend.exception.handler;

import com.booking.backend.domain.book.dto.BookingFailResponse;
import com.booking.backend.exception.dto.ErrorResponse;
import com.booking.backend.exception.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 예약 실패시 예외처리
    @ExceptionHandler({BookingTimeExceedException.class,
            BookingTimeLimitExceedException.class,
            OutsideBookingTimeException.class,
            InvalidBookingTimeException.class,
            MinParticipantsNotMetException.class,
            BookingConflictException.class,
            InvalidBookingMemberException.class,
            InvalidBookingRequestException.class
    })
    public ResponseEntity<BookingFailResponse> bookingBadRequestHandler(Exception e) {

        log.debug("⚠️ 예약 실패 | 실패 사유 : {}", e.getMessage());

        String message = e.getMessage();
        return ResponseEntity.status(400).body(new BookingFailResponse(message));
    }

    @ExceptionHandler({MemberNotFoundException.class,
            RoomNotFoundException.class,
            BookingNotFoundException.class,
            LockNotFoundException.class,
            TeamNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> notFoundExceptionHandler(Exception e) {

        log.debug("⚠️ 404 Not Found: {}", e.getMessage());

        ErrorResponse res = new ErrorResponse(
                404,
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(404).body(res);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accessDeniedExceptionHandler(AccessDeniedException e) {
        log.debug("⚠️ 403 Forbidden: {}", e.getMessage());
        ErrorResponse res = new ErrorResponse(403, "접근 권한이 없습니다.", LocalDateTime.now());
        return ResponseEntity.status(403).body(res);
    }

    // 연장 실패시 예외 처리
    @ExceptionHandler({
            ExtendNotAllowedException.class
    })
    public ResponseEntity<BookingFailResponse> extendFailExceptionHandler(Exception e) {

        log.debug("⚠️ 연장 실패 | 실패 사유 : {}", e.getMessage());

        String message = e.getMessage();
        return ResponseEntity.status(400).body(new BookingFailResponse(message));
    }

    @ExceptionHandler({
            InvalidCurrentPasswordException.class,
            InvalidNewPasswordException.class,
            InvalidLockTimeException.class,
            AlreadySameTeamException.class,
            InvalidMemberException.class
    })
    public ResponseEntity<ErrorResponse> badRequestExceptionHandler(Exception e) {

        log.debug("⚠️ 400 Bad Request : {}", e.getMessage());

        ErrorResponse res = new ErrorResponse(
                400,
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(400).body(res);
    }

    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<ErrorResponse> notificationExceptionHandler(NotificationException e){
        int status = e.getStatus().value();

        log.debug("{} : {}", status, e.getMessage());
        ErrorResponse res = new ErrorResponse(
                e.getStatus().value(),
                e.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(status).body(res);
    }

    @ExceptionHandler(TelegramNotLinkedException.class)
    public ResponseEntity<ErrorResponse> handleTelegramLinkedException(Exception e) {
        log.debug("⚠️ 422 Unprocessable Entity : {}", e.getMessage());
        ErrorResponse res = new ErrorResponse(422, e.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(422).body(res);
    }

}

