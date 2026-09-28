-- 방 번호 채번을 메모리에서 이 테이블로 옮깁니다.

-- 유일 제약을 걸기 전에 중복 이름을 정리합니다.
delete from counter_entity
 where id not in (select keep_id from (select min(id) as keep_id from counter_entity group by name) as keep);

-- 부르는 곳이 없던 POST /game/player 전용이었습니다.
delete from counter_entity where name = 'PLAYER_COUNTER';

insert into counter_entity (name, count)
select s.counter_name, s.counter_count
  from (select 'GAME_ROOM_COUNTER' as counter_name, 0 as counter_count) as s
 where not exists (select 1 from counter_entity where name = 'GAME_ROOM_COUNTER');

alter table counter_entity add constraint uk_counter_entity_name unique (name);
