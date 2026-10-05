package com.fungame.songquiz.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.fungame.songquiz.support.MutableClock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.util.ReflectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BroadcastCircuitBreakerTest {

    private static final int THRESHOLD = 3;
    private static final long COOLDOWN_MILLIS = 5_000;

    private MutableClock clock;
    private MeterRegistry meterRegistry;
    private BroadcastCircuitBreaker breaker;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-06T00:00:00Z"), ZoneId.of("UTC"));
        meterRegistry = new SimpleMeterRegistry();
        breaker = new BroadcastCircuitBreaker(clock, meterRegistry, THRESHOLD, COOLDOWN_MILLIS);
    }

    private void fail(int times) {
        for (int i = 0; i < times; i++) {
            breaker.allows();
            breaker.recordFailure();
        }
    }

    @Test
    @DisplayName("정상일 때는 통과시킨다.")
    void a_healthy_breaker_lets_everything_through() {
        assertThat(breaker.allows()).isTrue();

        breaker.recordSuccess();

        assertThat(breaker.allows()).isTrue();
    }

    @Test
    @DisplayName("연속 실패가 기준에 닿으면 막는다. 멈춘 Redis 에 2초씩 계속 물리지 않는다.")
    void consecutive_failures_open_the_breaker() {
        fail(THRESHOLD);

        assertThat(breaker.allows()).isFalse();
    }

    @Test
    @DisplayName("중간에 한 번 성공하면 실패 횟수가 초기화된다. 띄엄띄엄 나는 실패로는 막지 않는다.")
    void a_success_resets_the_streak() {
        fail(THRESHOLD - 1);
        breaker.recordSuccess();
        fail(THRESHOLD - 1);

        assertThat(breaker.allows()).isTrue();
    }

    @Test
    @DisplayName("쿨다운이 지나면 한 건을 흘려 보내 복구를 확인한다.")
    void after_the_cooldown_one_probe_goes_through() {
        fail(THRESHOLD);
        assertThat(breaker.allows()).isFalse();

        clock.plus(Duration.ofMillis(COOLDOWN_MILLIS + 1));

        assertThat(breaker.allows()).isTrue();
    }

    @Test
    @DisplayName("복구 확인에 실패하면 다시 막고 쿨다운을 새로 센다.")
    void a_failed_probe_opens_it_again() {
        fail(THRESHOLD);
        clock.plus(Duration.ofMillis(COOLDOWN_MILLIS + 1));

        assertThat(breaker.allows()).isTrue();
        breaker.recordFailure();

        assertThat(breaker.allows()).isFalse();
    }

    @Test
    @DisplayName("복구 확인에 성공하면 다시 통과시킨다.")
    void a_successful_probe_closes_it() {
        fail(THRESHOLD);
        clock.plus(Duration.ofMillis(COOLDOWN_MILLIS + 1));
        breaker.allows();
        breaker.recordSuccess();

        assertThat(breaker.allows()).isTrue();
    }

    @Test
    @DisplayName("막혀 있는 동안 버린 것을 센다. 조용히 사라지면 안 된다.")
    void what_it_blocks_is_counted() {
        fail(THRESHOLD);

        breaker.allows();
        breaker.allows();

        assertThat(meterRegistry.counter("fungame.broadcast.dropped", "reason", "circuit_open").count())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("상태를 0 으로 되돌리면 닫힌 것으로 읽힌다. 테스트 사이 정리가 리플렉션으로 0 을 넣는다.")
    void zeroing_the_state_reads_as_closed() {
        fail(THRESHOLD);
        assertThat(breaker.allows()).isFalse();

        ReflectionUtils.doWithFields(BroadcastCircuitBreaker.class, field -> {
            ReflectionUtils.makeAccessible(field);
            Object value = ReflectionUtils.getField(field, breaker);
            if (value instanceof AtomicInteger number) {
                number.set(0);
            }
            if (value instanceof AtomicLong number) {
                number.set(0);
            }
        });

        assertThat(breaker.allows()).isTrue();
    }
}
