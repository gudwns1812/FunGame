delete from promotion_request where member_id <> 1;
delete from member where id <> 1;

delete from game_room_member;
delete from game_room;

alter table member add column email varchar(255) not null default '';
update member set email = 'gudwns1812@naver.com' where id = 1;
alter table member alter column email drop default;
alter table member add constraint uk_member_email unique (email);

create table password_reset_token (
    id bigint not null auto_increment,
    member_id bigint not null,
    token_hash char(64) not null,
    expires_at datetime(6) not null,
    used_at datetime(6),
    created_at datetime(6) not null,
    primary key (id),
    constraint uk_password_reset_token_hash unique (token_hash),
    constraint fk_password_reset_token_member foreign key (member_id) references member (id)
) engine = InnoDB;

create index idx_password_reset_token_member on password_reset_token (member_id);
