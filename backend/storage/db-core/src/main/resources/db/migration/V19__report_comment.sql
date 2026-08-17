-- 신고에 대한 관리자 답변. 신고자에게 그대로 보인다.
-- 신고를 지우면 답변만 남아 갈 곳이 없으므로 함께 지운다.
create table report_comment (
    id bigint not null auto_increment,
    report_id bigint not null,
    member_id bigint not null,
    content text not null,
    created_at datetime(6) not null,
    primary key (id),
    constraint fk_report_comment_report foreign key (report_id) references report (id) on delete cascade,
    constraint fk_report_comment_member foreign key (member_id) references member (id)
) engine = InnoDB;

create index idx_report_comment_report on report_comment (report_id, created_at);
