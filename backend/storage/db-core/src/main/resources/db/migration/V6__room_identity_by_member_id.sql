-- 방·게임의 플레이어 식별자를 닉네임에서 member_id 로 바꾼다.
-- 방은 30분 유휴면 정리되는 임시 데이터이므로 백필하지 않고 비운다. (V4 와 같은 방식)
delete from game_room_member;
delete from game_room;

alter table game_room drop column host_nickname;
alter table game_room add column host_member_id bigint not null;

-- (game_room_id, nickname) 유니크 키가 game_room_id 외래키의 인덱스 역할을 겸하고 있어
-- 새 유니크 키를 먼저 만든 뒤에 지운다.
alter table game_room_member add column member_id bigint not null;
alter table game_room_member add constraint uk_game_room_member_member unique (game_room_id, member_id);
alter table game_room_member drop constraint uk_game_room_member_nickname;
alter table game_room_member drop column nickname;
alter table game_room_member add constraint fk_game_room_member_member foreign key (member_id) references member (id);
