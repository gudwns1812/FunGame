-- CS 퀴즈 방의 출제 난이도 상한. 고른 값 이하 난이도의 문제가 모두 후보가 된다.
-- 기존 방은 난이도 구분 없이 전체 문제를 뽑아 왔으므로 그와 같은 HARD 로 채운다.
alter table game_room add column cs_difficulty enum ('EASY', 'NORMAL', 'HARD') not null default 'HARD';
