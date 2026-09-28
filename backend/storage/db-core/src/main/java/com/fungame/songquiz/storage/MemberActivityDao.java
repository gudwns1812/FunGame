package com.fungame.songquiz.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class MemberActivityDao {

    // insert ignore 는 새로 넣으면 1, 이미 있으면 0 이다.
    // on duplicate key update 로 바꾸면 갱신했을 때 2 가 나와 "새로 생겼는가" 를 이 값으로 못 읽는다.
    private static final String RECORD_ACTIVE = "insert ignore into member_daily_active (member_id, active_on) values (?, ?)";

    private static final String COUNT_ACTIVE_ON = "select count(*) from member_daily_active where active_on = ?";

    private final JdbcTemplate jdbcTemplate;

    public boolean recordActive(Long memberId, LocalDate date) {
        return jdbcTemplate.update(RECORD_ACTIVE, memberId, date) == 1;
    }

    public long countActiveOn(LocalDate date) {
        Long count = jdbcTemplate.queryForObject(COUNT_ACTIVE_ON, Long.class, date);

        return count == null ? 0 : count;
    }
}
