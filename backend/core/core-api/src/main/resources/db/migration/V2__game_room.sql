create table game_room (
    id bigint not null auto_increment,
    title varchar(255) not null,
    game_type enum ('CS', 'HANGMAN', 'NONE', 'SONG') not null,
    status enum ('PLAYING', 'WAITING') not null,
    max_player integer not null,
    host_nickname varchar(100) not null,
    category enum ('BALLAD', 'DEFAULT', 'KPOP', 'OST', 'POP', 'RAP', 'TOTAL'),
    total_round integer not null,
    difficulty integer not null,
    last_activity_time datetime(6) not null,
    primary key (id)
) engine = InnoDB;

create table game_room_member (
    id bigint not null auto_increment,
    game_room_id bigint not null,
    nickname varchar(100) not null,
    ready bit not null,
    joined_at datetime(6) not null,
    primary key (id),
    constraint uk_game_room_member_nickname unique (game_room_id, nickname),
    constraint fk_game_room_member_room foreign key (game_room_id) references game_room (id)
) engine = InnoDB;
