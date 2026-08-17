-- content_id 는 신고 대상 식별자이지만 FK 를 걸지 않는다.
-- 대상 행이 지워져도 신고 내용은 남아야 하고, 가리키는 테이블이 게임 종류마다 다르다.
create table report (
    id bigint not null auto_increment,
    member_id bigint not null,
    source varchar(20) not null,
    reason varchar(30) not null,
    detail text,
    game_type varchar(20),
    quiz_category varchar(255),
    content_id bigint,
    room_id bigint,
    current_round integer,
    total_round integer,
    quiz_content text,
    quiz_answer text,
    quiz_hint text,
    status varchar(20) not null,
    created_at datetime(6) not null,
    primary key (id),
    constraint fk_report_member foreign key (member_id) references member (id)
) engine = InnoDB;

create index idx_report_member_created_at on report (member_id, created_at);
create index idx_report_content on report (content_id, reason);
