# workspace-backend CLAUDE.md

회의실 예약 시스템 백엔드 (Spring Boot + JPA + Spring Security)

---

## 협업 규칙

- 백엔드 코드 수정은 먼저 사용자에게 물어볼 것
- 테스트/서버 인스턴스를 묻지 않고 띄우지 말 것
- 코드 수정 후 보고만 할 것 — 테스트는 사용자가 직접 함

---

## 프로젝트 구조

```
src/main/java/com/booking/backend/
├── domain/
│   ├── book/
│   │   ├── entity/
│   │   │   ├── Booking.java          # @OneToMany(mappedBy="booking") bookingMembers
│   │   │   ├── BookingMember.java
│   │   │   └── BookStatus.java
│   │   └── repository/
│   │       └── BookingRepository.java  # JOIN FETCH로 N+1 방지
│   ├── room/
│   │   ├── Room.java
│   │   ├── RoomLock.java
│   │   └── dto/
│   │       └── BookingTimetableResponse.java  # memberNames: List<String>
│   ├── user/
│   │   ├── entity/
│   │   │   ├── Member.java
│   │   │   └── Team.java
│   │   └── ...
│   └── ...
└── ...
```

---

## 주요 구현 사항 (이전 작업 이력)

### BookingTimetableResponse
- `memberNames: List<String>` 필드 포함 (LOCK일 경우 null)
- Booking 생성자: `booking.getMemberNames()` 호출

### Booking.java
```java
@OneToMany(mappedBy = "booking")
private List<BookingMember> bookingMembers;

public List<String> getMemberNames() {
    return bookingMembers.stream().map(b -> b.getMember().getName()).toList();
}
```

### BookingRepository
- `JOIN FETCH b.bookingMembers bm JOIN FETCH bm.member` — N+1 방지

### 인증
- Spring Security 세션 기반 (JSESSIONID)
- 세션 만료 시 401 반환 → 프론트 `apiFetch`가 처리

---

## 기술 스택

- Spring Boot 3
- Spring Security (세션 기반)
- Spring Data JPA
- MySQL
- Telegram Bot (알림)
