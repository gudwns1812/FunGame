create table game_record (
    game_id char(36) not null,
    game_type varchar(20) not null,
    started_at datetime(6) not null,
    ended_at datetime(6) not null,
    started_player_count integer not null,
    primary key (game_id)
) engine = InnoDB;

create table game_record_player (
    game_id char(36) not null,
    member_id bigint not null,
    game_type varchar(20) not null,
    won boolean not null,
    player_rank integer,
    score integer,
    left_game boolean not null,
    primary key (game_id, member_id),
    constraint fk_game_record_player_game foreign key (game_id) references game_record (game_id),
    constraint fk_game_record_player_member foreign key (member_id) references member (id)
) engine = InnoDB;

create index idx_game_record_player_member_type on game_record_player (member_id, game_type);
