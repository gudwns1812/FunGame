-- local 프로파일 전용 시드. application-local.yml 에서만 참조합니다.

-- 방/플레이어 번호 채번용 카운터. 없으면 방 생성이 NPE 로 실패합니다.
INSERT INTO counter_entity (name, count) VALUES ('GAME_ROOM_COUNTER', 0);
INSERT INTO counter_entity (name, count) VALUES ('PLAYER_COUNTER', 0);

-- CS 퀴즈를 바로 돌려볼 수 있을 만큼의 최소 문제.
-- 방 난이도는 상한선이라 EASY 방은 EASY 만, NORMAL 방은 EASY+NORMAL 이 출제됩니다.
-- 난이도별로 확인할 수 있도록 세 단계를 모두 넣어 둡니다.
-- 음악 퀴즈는 유튜브 링크가 필요해 시드에 넣지 않았습니다. 관리자 화면에서 등록하세요.
INSERT INTO computer_science_entity (field, content, answers, explanation, difficulty) VALUES
 ('네트워크', 'TCP 3-way handshake 의 두 번째 단계에서 서버가 보내는 플래그 조합은?', 'synack', 'SYN+ACK 를 보낸다.', 'EASY'),
 ('운영체제', '프로세스 간 통신에서 공유 자원 접근을 제어하는 정수 기반 동기화 도구는?', '세마포어', '세마포어(Semaphore).', 'EASY'),
 ('자료구조', 'FIFO 방식으로 동작하는 선형 자료구조는?', '큐', '큐(Queue).', 'EASY'),
 ('데이터베이스', '트랜잭션의 ACID 중 A 가 뜻하는 것은?', '원자성', '원자성(Atomicity).', 'EASY'),
 ('네트워크', 'HTTP 상태코드 404 의 의미는?', 'notfound', 'Not Found.', 'EASY'),
 ('운영체제', '페이지 교체에서 앞으로 가장 늦게 쓰일 페이지를 내보내는 이론상 최적 알고리즘은?', 'belady,벨레이디', 'Belady 의 최적 알고리즘.', 'NORMAL'),
 ('데이터베이스', '갱신 손실을 막기 위해 읽은 데이터에도 공유 락을 거는 격리 수준은?', 'repeatableread', 'REPEATABLE READ.', 'NORMAL'),
 ('자료구조', '삽입과 삭제가 O(log n) 이고 항상 완전 이진 트리를 유지하는 자료구조는?', '힙', '힙(Heap).', 'NORMAL'),
 ('네트워크', 'TCP 에서 혼잡 윈도를 지수적으로 늘리는 초기 단계의 이름은?', 'slowstart,느린시작', 'Slow Start.', 'NORMAL'),
 ('운영체제', '실행 중 프로세스의 가상 주소를 물리 주소로 바꾸는 캐시성 하드웨어는?', 'tlb', 'Translation Lookaside Buffer.', 'HARD'),
 ('데이터베이스', '스냅샷 격리에서 두 트랜잭션이 서로의 읽은 값을 뒤집어 쓰는 이상 현상은?', 'writeskew,쓰기편향', 'Write Skew.', 'HARD'),
 ('자료구조', '최악에도 O(log n) 을 보장하려고 색으로 균형을 맞추는 이진 탐색 트리는?', '레드블랙트리,redblacktree', 'Red-Black Tree.', 'HARD');
