-- 별도 인덱스를 두지 않는다. 하루치 집계(where active_on = ?)는 DAU × 일수 규모라 풀스캔으로
-- 충분하고, 회원별 리텐션 조회는 기본키의 선두 컬럼을 탄다.
create table member_daily_active (
    member_id bigint not null,
    active_on date not null,
    primary key (member_id, active_on),
    constraint fk_member_daily_active_member foreign key (member_id) references member (id)
) engine = InnoDB;
