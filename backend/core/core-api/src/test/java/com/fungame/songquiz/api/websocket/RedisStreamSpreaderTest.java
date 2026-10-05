package com.fungame.songquiz.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.support.MutableClock;
import com.fungame.songquiz.support.config.InstanceId;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.QueryTimeoutException;

class RedisStreamSpreaderTest {

    private static final String DESTINATION = "/topic/room/7";
    private static final int THRESHOLD = 3;
    private static final long COOLDOWN_MILLIS = 5_000;

    private MutableClock clock;
    private MeterRegistry meterRegistry;
    private BroadcastStream stream;
    private RedisStreamSpreader spreader;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-06T00:00:00Z"), ZoneId.of("UTC"));
        meterRegistry = new SimpleMeterRegistry();
        stream = mock(BroadcastStream.class);
        spreader = new RedisStreamSpreader(
                new BroadcastMessages(new InstanceId("instance-a"), new ObjectMapper(), meterRegistry),
                stream,
                new BroadcastCircuitBreaker(clock, meterRegistry, THRESHOLD, COOLDOWN_MILLIS));
    }

    private void spread() {
        spreader.spread(DESTINATION, BroadcastMessage.EVERYONE, Map.of("type", "ROUND_START"));
    }

    private void breakIt() {
        doThrow(new QueryTimeoutException("Redis command timed out")).when(stream).write(any());
        for (int i = 0; i < THRESHOLD; i++) {
            spread();
        }
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
    @DisplayName("연속 실패가 쌓이면 더 이상 Redis 를 건드리지 않는다. 멈춘 Redis 에 2초씩 계속 물지 않는다.")
    void it_stops_touching_redis_after_repeated_failures() {
        breakIt();

        spread();
        spread();

        verify(stream, times(THRESHOLD)).write(any());
    }

    @Test
    @DisplayName("쿨다운이 지나면 한 건으로 복구를 확인한다.")
    void it_probes_once_after_the_cooldown() {
        breakIt();
        clock.plus(Duration.ofMillis(COOLDOWN_MILLIS + 1));

        spread();

        verify(stream, times(THRESHOLD + 1)).write(any());
    }

    @Test
    @DisplayName("직렬화하지 못하면 스트림을 건드리지 않는다.")
    void an_unserializable_payload_never_reaches_the_stream() {
        spreader.spread(DESTINATION, BroadcastMessage.EVERYONE, new Object());

        verify(stream, never()).write(any());
    }
}
