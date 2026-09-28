-- 방 번호 채번을 메모리(AtomicLong)에서 이 테이블로 옮깁니다.
-- 재기동해도 번호가 이어지고, 인스턴스가 늘어도 같은 번호가 두 번 나가지 않습니다.

-- name 에 유일 제약이 없어 같은 이름이 여러 행 있을 수 있습니다.
-- findByName 이 그 상태에서 터지므로 가장 오래된 행만 남기고 정리합니다.
-- (자기 테이블을 직접 서브쿼리로 참조하면 MySQL 이 거부해 파생 테이블로 감쌉니다)
delete from counter_entity
 where id not in (select keep_id from (select min(id) as keep_id from counter_entity group by name) as keep);

-- 더 이상 쓰지 않습니다. 부르는 곳이 없던 POST /game/player 전용이었습니다.
delete from counter_entity where name = 'PLAYER_COUNTER';

insert into counter_entity (name, count)
select s.counter_name, s.counter_count
  from (select 'GAME_ROOM_COUNTER' as counter_name, 0 as counter_count) as s
 where not exists (select 1 from counter_entity where name = 'GAME_ROOM_COUNTER');

alter table counter_entity add constraint uk_counter_entity_name unique (name);
