-- 로그인/예약 테스트용 임시 데이터
-- 모든 계정의 비밀번호(평문): password123

INSERT INTO teams (id, name, ta_id) VALUES
  (1, '1팀', 1),
  (2, '2팀', 1);

INSERT INTO members (id, team_id, login_id, name, password, role) VALUES
  (1, NULL, 'ta01', '박선생', '$2a$10$ntYHhpaWQFJvDUKp/zq4V.WnNhjyRmR8rsMnFC3rKxdqXpV/Vat0C', 'TA'),
  (2, 1, '20260101', '김학생', '$2a$10$XQWCuba6npPf7NxWfzFvi.AC2EQw.r5QSuN9wEyTtEkxt3gOMTDHm', 'STUDENT'),
  (3, 1, '20260102', '이학생', '$2a$10$FqJXdzMz6DDhOp94lpRs3OwX4JnT0TMfB.0nViK0LUBvcUj4BeVfG', 'STUDENT'),
  (4, 1, '20260103', '박학생', '$2a$10$gqkrd1XildEv2/1bFwu0M.xypVZR0ZUJMtYz.EbuWpSKINEAyQX16', 'STUDENT'),
  (5, 1, '20260104', '최학생', '$2a$10$q7tKIBvin8gSELtf5I2l0efAYoTOqTHYc06P35N06MAlbm4xd.8UC', 'STUDENT'),
  (6, 2, '20260201', '정학생', '$2a$10$HyeUoAKCRVS1lIFmIpDEeuVHtVFa38khRybAvYc4vLS0kQ2P4cfSa', 'STUDENT'),
  (7, 2, '20260202', '강학생', '$2a$10$YO2bqyTbZDWFnmpArXuMWu5F2qTC4i3dsMoZd38ZcM6zCjVeBEisW', 'STUDENT'),
  (8, 2, '20260203', '조학생', '$2a$10$ZwRVEcLW3z.VZOL9fvraCOovjq3BEvisgKppDVsxaJK7Xobg9Lqxe', 'STUDENT'),
  (9, 2, '20260204', '윤학생', '$2a$10$B9L2ecCLerbp14S71NE8q.TpV6aOARe05sWKBf.OZHiDOcwGamOTy', 'STUDENT');

INSERT INTO rooms (id, name) VALUES
  (1, '회의실 1'),
  (2, '회의실 2');

-- 오늘 날짜 기준으로 타임테이블 데모용 예약 5건 (정상/조정됨/취소/조기반납 케이스 포함)
INSERT INTO bookings (id, team_id, room_id, member_id, start_time, end_time, booking_status,
                       created_at, updated_at, original_start_time, original_end_time, adjusted_at, adjusted_by_id) VALUES
  -- 1팀, 회의실1, 09:00~10:00, 정상 예약
  (1, 1, 1, 2,
   DATEADD('HOUR', 9, CAST(CURRENT_DATE AS TIMESTAMP)),
   DATEADD('HOUR', 10, CAST(CURRENT_DATE AS TIMESTAMP)),
   'BOOKED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, NULL, NULL),

  -- 2팀, 회의실1, 원래 11:00~11:30이었는데 TA가 11:00~12:00으로 조정함
  (2, 2, 1, 6,
   DATEADD('HOUR', 11, CAST(CURRENT_DATE AS TIMESTAMP)),
   DATEADD('HOUR', 12, CAST(CURRENT_DATE AS TIMESTAMP)),
   'BOOKED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   DATEADD('HOUR', 11, CAST(CURRENT_DATE AS TIMESTAMP)),
   DATEADD('MINUTE', 30, DATEADD('HOUR', 11, CAST(CURRENT_DATE AS TIMESTAMP))),
   DATEADD('HOUR', 8, CAST(CURRENT_DATE AS TIMESTAMP)),
   1),

  -- 2팀, 회의실1, 13:00~14:00, 정상 예약
  (3, 2, 1, 6,
   DATEADD('HOUR', 13, CAST(CURRENT_DATE AS TIMESTAMP)),
   DATEADD('HOUR', 14, CAST(CURRENT_DATE AS TIMESTAMP)),
   'BOOKED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, NULL, NULL),

  -- 1팀, 회의실1, 16:00~16:30, TA가 취소함
  (4, 1, 1, 2,
   DATEADD('HOUR', 16, CAST(CURRENT_DATE AS TIMESTAMP)),
   DATEADD('MINUTE', 30, DATEADD('HOUR', 16, CAST(CURRENT_DATE AS TIMESTAMP))),
   'CANCELLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, NULL, NULL),

  -- 2팀, 회의실2, 10:00~11:00 예정이었으나 10:45에 조기반납
  (5, 2, 2, 6,
   DATEADD('HOUR', 10, CAST(CURRENT_DATE AS TIMESTAMP)),
   DATEADD('MINUTE', 45, DATEADD('HOUR', 10, CAST(CURRENT_DATE AS TIMESTAMP))),
   'EARLY_RETURNED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, NULL, NULL);

INSERT INTO booking_members (book_id, member_id) VALUES
  (1, 2), (1, 3), (1, 4), (1, 5),
  (2, 6), (2, 7), (2, 8), (2, 9),
  (3, 6), (3, 7), (3, 8), (3, 9),
  (4, 2), (4, 3), (4, 4), (4, 5),
  (5, 6), (5, 7), (5, 8), (5, 9);

-- TA(ta01)가 회의실2를 15:00~16:00 사무실 업무로 락
INSERT INTO room_locks (id, room_id, member_id, start_time, end_time, reason, created_at) VALUES
  (1, 2, 1,
   DATEADD('HOUR', 15, CAST(CURRENT_DATE AS TIMESTAMP)),
   DATEADD('HOUR', 16, CAST(CURRENT_DATE AS TIMESTAMP)),
   '사무실 회의', CURRENT_TIMESTAMP);

-- 위에서 id를 직접 지정해서 넣었기 때문에, IDENTITY 컬럼의 다음 값 카운터가
-- 여전히 1부터 시작한 상태로 남아있음 -> 이후 API로 새로 생성하는 행이
-- 시드 데이터의 id와 충돌해서 PK violation이 남 (예: 새 예약 생성 시 500 에러).
-- 시드 데이터의 최대 id + 1로 카운터를 맞춰준다.
ALTER TABLE teams ALTER COLUMN id RESTART WITH 3;
ALTER TABLE members ALTER COLUMN id RESTART WITH 10;
ALTER TABLE rooms ALTER COLUMN id RESTART WITH 3;
ALTER TABLE bookings ALTER COLUMN id RESTART WITH 6;
ALTER TABLE room_locks ALTER COLUMN id RESTART WITH 2;
