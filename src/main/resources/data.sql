-- 로그인 테스트용 임시 데이터
-- 모든 계정의 비밀번호(평문): password123

INSERT INTO teams (id, name, ta_id) VALUES (1, '1팀', NULL);

INSERT INTO members (id, team_id, login_id, name, password, role) VALUES
  (1, NULL, 'ta01', '박선생', '$2a$10$ntYHhpaWQFJvDUKp/zq4V.WnNhjyRmR8rsMnFC3rKxdqXpV/Vat0C', 'TA'),
  (2, 1, '20260101', '김학생', '$2a$10$XQWCuba6npPf7NxWfzFvi.AC2EQw.r5QSuN9wEyTtEkxt3gOMTDHm', 'STUDENT'),
  (3, 1, '20260102', '이학생', '$2a$10$FqJXdzMz6DDhOp94lpRs3OwX4JnT0TMfB.0nViK0LUBvcUj4BeVfG', 'STUDENT');
