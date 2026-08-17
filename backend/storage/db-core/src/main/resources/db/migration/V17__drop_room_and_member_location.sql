-- 방과 회원 위치는 재시작하면 복구하지 않는 휘발성 상태이므로 인메모리로 옮긴다.
-- 남아 있던 행은 어차피 기동 시점에 비워지던 데이터라 백필하지 않는다. (V6 과 같은 방식)
drop table if exists game_room_member;
drop table if exists game_room;

-- current_room_id 를 지우면 idx_member_current_room 도 함께 사라진다.
alter table member drop column current_room_id;
alter table member drop column status;
