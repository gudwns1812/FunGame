-- local 프로파일 전용 시드. application-local.yml 에서만 참조합니다.

-- 방/플레이어 번호 채번용 카운터. 없으면 방 생성이 NPE 로 실패합니다.
INSERT INTO counter_entity (name, count) VALUES ('GAME_ROOM_COUNTER', 0);
INSERT INTO counter_entity (name, count) VALUES ('PLAYER_COUNTER', 0);

-- CS 퀴즈를 바로 돌려볼 수 있을 만큼의 최소 문제.
-- 음악 퀴즈는 유튜브 링크가 필요해 시드에 넣지 않았습니다. 관리자 화면에서 등록하세요.
INSERT INTO computer_science_entity (field, content, answers, explanation, difficulty) VALUES
 ('네트워크', 'TCP 3-way handshake 의 두 번째 단계에서 서버가 보내는 플래그 조합은?', 'synack', 'SYN+ACK 를 보낸다.', 'EASY'),
 ('운영체제', '프로세스 간 통신에서 공유 자원 접근을 제어하는 정수 기반 동기화 도구는?', '세마포어', '세마포어(Semaphore).', 'EASY'),
 ('자료구조', 'FIFO 방식으로 동작하는 선형 자료구조는?', '큐', '큐(Queue).', 'EASY'),
 ('데이터베이스', '트랜잭션의 ACID 중 A 가 뜻하는 것은?', '원자성', '원자성(Atomicity).', 'EASY'),
 ('네트워크', 'HTTP 상태코드 404 의 의미는?', 'notfound', 'Not Found.', 'EASY');
