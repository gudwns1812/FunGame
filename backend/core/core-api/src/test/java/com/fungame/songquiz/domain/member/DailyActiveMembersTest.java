package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.storage.MemberActivityDao;
import com.fungame.songquiz.support.MutableClock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DailyActiveMembersTest {

    private static final ZoneId 서울 = ZoneId.of("Asia/Seoul");
    private static final Instant 정오 = Instant.parse("2026-08-18T03:00:00Z");

    private StubDao dao;
    private MutableClock clock;
    private MeterRegistry registry;
    private DailyActiveMembers dailyActiveMembers;

    @BeforeEach
    void setUp() {
        dao = new StubDao();
        clock = new MutableClock(정오, 서울);
        registry = new SimpleMeterRegistry();
        dailyActiveMembers = new DailyActiveMembers(dao, clock, registry);
    }

    private double firstSeenCount() {
        return registry.get(DailyActiveMembers.FIRST_SEEN_METER).counter().count();
    }

    @Test
    @DisplayName("계측을 시작하는 순간 카운터가 0 으로 존재한다.")
    void registersCounterUpFront() {
        assertThat(firstSeenCount()).isZero();
    }

    @Test
    @DisplayName("그날 처음 온 회원이면 카운터가 오른다.")
    void countsFirstVisitOfTheDay() {
        dailyActiveMembers.record(1L);

        assertThat(firstSeenCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 날 다시 접속하면 DB 를 보지 않고 카운터도 그대로다.")
    void skipsDatabaseOnSecondVisitOfSameDay() {
        dailyActiveMembers.record(1L);
        dao.recorded.clear();

        dailyActiveMembers.record(1L);

        assertThat(dao.recorded).isEmpty();
        assertThat(firstSeenCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("날짜가 바뀌면 같은 회원이라도 다시 센다.")
    void countsAgainAfterMidnight() {
        dailyActiveMembers.record(1L);

        clock.plus(Duration.ofDays(1));
        dailyActiveMembers.record(1L);

        assertThat(dao.recorded).contains(LocalDate.of(2026, 8, 19));
        assertThat(firstSeenCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("DB 에 이미 그날 행이 있으면 카운터를 올리지 않는다.")
    void doesNotCountWhenRowAlreadyExists() {
        dao.inserts = false;

        dailyActiveMembers.record(1L);

        assertThat(firstSeenCount()).isZero();
    }

    @Test
    @DisplayName("DB 가 실패해도 접속 처리를 막지 않고, 다음 접속에 다시 시도한다.")
    void swallowsDatabaseFailureAndRetriesNextTime() {
        dao.fails = true;
        dailyActiveMembers.record(1L);

        dao.fails = false;
        dailyActiveMembers.record(1L);

        assertThat(firstSeenCount()).isEqualTo(1);
    }

    private static class StubDao extends MemberActivityDao {

        private final List<LocalDate> recorded = new ArrayList<>();
        private boolean inserts = true;
        private boolean fails = false;

        private StubDao() {
            super(null);
        }

        @Override
        public boolean recordActive(Long memberId, LocalDate date) {
            if (fails) {
                throw new IllegalStateException("DB 가 응답하지 않는다");
            }

            recorded.add(date);
            return inserts;
        }
    }
}
