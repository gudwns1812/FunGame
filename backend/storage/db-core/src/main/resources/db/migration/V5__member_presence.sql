alter table member add column status varchar(20) not null default 'LOBBY';
alter table member add column current_room_id bigint;

create index idx_member_current_room on member (current_room_id);
