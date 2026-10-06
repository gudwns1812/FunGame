package com.fungame.songquiz.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.support.config.InstanceId;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.QueryTimeoutException;

class RedisStreamSpreaderTest {

    private static final String DESTINATION = "/topic/room/7";
    private static final int MINIMUM_CALLS = 3;
    private static final Duration SLOW_CALL = Duration.ofMillis(100);

    private MeterRegistry meterRegistry;
    private BroadcastStream stream;
    private CircuitBreaker breaker;
    private RedisStreamSpreader spreader;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        stream = mock(BroadcastStream.class);
        breaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(5)
                .minimumNumberOfCalls(MINIMUM_CALLS)
                .failureRateThreshold(50)
                .slowCallDurationThreshold(SLOW_CALL)
                .slowCallRateThreshold(50)
                .permittedNumberOfCallsInHalfOpenState(1)
                .build());
        spreader = new RedisStreamSpreader(
                new BroadcastMessageFactory(new InstanceId("instance-a"), new ObjectMapper(), meterRegistry),
                stream, breaker, meterRegistry);
    }

    private void spread() {
        spreader.spread(DESTINATION, BroadcastMessage.EVERYONE, Map.of("type", "ROUND_START"));
    }

    private void breakIt() {
        doThrow(new QueryTimeoutException("Redis command timed out")).when(stream).write(any());
        for (int i = 0; i < MINIMUM_CALLS; i++) {
            spread();
        }
    }

    private double droppedOpen() {
        return meterRegistry.counter("fungame.broadcast.dropped", "reason", "circuit_open").count();
    }

    @Test
    @DisplayName("스트림에 메시지를 쓴다.")
    void it_writes_to_the_stream() {
        spread();

        ArgumentCaptor<BroadcastMessage> written = ArgumentCaptor.captor();
        verify(stream).write(written.capture());

        assertThat(written.getValue().destination()).isEqualTo(DESTINATION);
        assertThat(written.getValue().payload()).isEqualTo("{\"type\":\"ROUND_START\"}");
    }

    @Test
    @DisplayName("쓰기가 실패해도 예외가 호출자에게 나가지 않는다.")
    void a_failed_write_does_not_reach_the_caller() {
        doThrow(new QueryTimeoutException("Redis command timed out")).when(stream).write(any());

        spread();

        verify(stream).write(any());
    }

    @Test
    @DisplayName("연속 실패가 쌓이면 더 이상 Redis 를 건드리지 않는다. 멈춘 Redis 에 계속 물지 않는다.")
    void it_stops_touching_redis_once_the_breaker_opens() {
        breakIt();

        spread();
        spread();

        verify(stream, times(MINIMUM_CALLS)).write(any());
        assertThat(droppedOpen()).isEqualTo(2);
    }

    @Test
    @DisplayName("실패하지 않아도 느리기만 하면 막는다. 멈춘 Redis 는 실패가 아니라 느림으로 먼저 온다.")
    void slow_writes_open_the_breaker_even_without_failing() {
        doAnswer(invocation -> {
            TimeUnit.MILLISECONDS.sleep(SLOW_CALL.toMillis() + 50);
            return null;
        }).when(stream).write(any());

        for (int i = 0; i < MINIMUM_CALLS; i++) {
            spread();
        }

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("쿨다운 뒤에는 한 건으로 복구를 확인한다.")
    void it_probes_once_when_half_open() {
        breakIt();

        breaker.transitionToHalfOpenState();
        spread();

        verify(stream, times(MINIMUM_CALLS + 1)).write(any());
    }

    @Test
    @DisplayName("직렬화하지 못하면 스트림을 건드리지 않는다.")
    void an_unserializable_payload_never_reaches_the_stream() {
        spreader.spread(DESTINATION, BroadcastMessage.EVERYONE, new Object());

        verify(stream, never()).write(any());
    }
}
