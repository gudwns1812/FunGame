package com.fungame.songquiz.domain.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RoundClockTest {

    private static final Duration ROUND_LENGTH = Duration.ofSeconds(30);
    private static final Duration UNTIL_HINT_OPENS = Duration.ofSeconds(20);
    private static final long TICK_TOLERANCE_MILLIS = 500L;

    private final RoundClock roundClock = new RoundClock();

    @Test
    @DisplayName("라운드는 30초이고 힌트는 시작 20초 뒤에 열린다.")
    void round_lasts_thirty_seconds_and_opens_hint_at_twenty() {
        assertThat(roundClock.length()).isEqualTo(ROUND_LENGTH);
        assertThat(roundClock.untilHintOpens()).isEqualTo(UNTIL_HINT_OPENS);
    }

    @Test
    @DisplayName("켜지 않은 시계는 남은 시간이 없다.")
    void reports_no_time_left_before_start() {
        assertThat(roundClock.remainingMillis()).isZero();
    }

    @Test
    @DisplayName("켜자마자는 라운드 길이만큼 남아 있다.")
    void reports_full_length_right_after_start() {
        roundClock.start();

        assertThat(roundClock.remainingMillis())
                .isBetween(ROUND_LENGTH.toMillis() - TICK_TOLERANCE_MILLIS, ROUND_LENGTH.toMillis());
    }

    @Test
    @DisplayName("멈춘 시계는 남은 시간이 없다 — 라운드 사이에는 셀 시간이 없다.")
    void reports_no_time_left_after_stop() {
        roundClock.start();
        roundClock.stop();

        assertThat(roundClock.remainingMillis()).isZero();
    }

    @Test
    @DisplayName("라운드 길이를 넘겨도 남은 시간은 0 아래로 내려가지 않는다.")
    void never_reports_negative_time_left() {
        startedAgo(ROUND_LENGTH.plusSeconds(10));

        assertThat(roundClock.remainingMillis()).isZero();
    }

    @Test
    @DisplayName("흐른 만큼 남은 시간이 줄어든다.")
    void counts_down_as_time_passes() {
        startedAgo(Duration.ofSeconds(12));

        assertThat(roundClock.remainingMillis())
                .isBetween(ROUND_LENGTH.minusSeconds(12).toMillis() - TICK_TOLERANCE_MILLIS,
                        ROUND_LENGTH.minusSeconds(12).toMillis());
    }

    private void startedAgo(Duration elapsed) {
        roundClock.start();
        ReflectionTestUtils.setField(roundClock, "startedAt", Instant.now().minus(elapsed));
    }
}
