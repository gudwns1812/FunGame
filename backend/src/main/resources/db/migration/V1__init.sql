create table member (
    id bigint not null auto_increment,
    login_id varchar(50) not null,
    password varchar(255) not null,
    nickname varchar(100) not null,
    role enum ('ADMIN', 'MASTER', 'USER') not null,
    created_at datetime(6) not null,
    primary key (id),
    constraint uk_member_login_id unique (login_id),
    constraint uk_member_nickname unique (nickname)
) engine = InnoDB;

create table promotion_request (
    id bigint not null auto_increment,
    member_id bigint not null,
    status enum ('APPROVED', 'PENDING', 'REJECTED') not null,
    created_at datetime(6) not null,
    processed_at datetime(6),
    primary key (id),
    constraint fk_promotion_request_member foreign key (member_id) references member (id)
) engine = InnoDB;

create table song_entity (
    id bigint not null auto_increment,
    title varchar(255) not null,
    singer varchar(255) not null,
    categories json,
    release_date date not null,
    video_link varchar(255) not null,
    play_seconds integer not null,
    answers varchar(255),
    hint varchar(255),
    primary key (id)
) engine = InnoDB;

create table computer_science_entity (
    id bigint not null auto_increment,
    field varchar(100),
    content TEXT,
    answers varchar(255),
    explanation TEXT,
    difficulty enum ('EASY', 'HARD', 'NORMAL'),
    primary key (id)
) engine = InnoDB;

create table counter_entity (
    id bigint not null auto_increment,
    name varchar(255),
    count bigint,
    primary key (id)
) engine = InnoDB;
