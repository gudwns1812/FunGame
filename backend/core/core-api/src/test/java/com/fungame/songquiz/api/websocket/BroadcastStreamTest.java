package com.fungame.songquiz.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fungame.songquiz.support.MutableClock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class BroadcastStreamTest {

    private static final String DESTINATION = "/topic/room/7";

    private StringRedisTemplate redisTemplate;
    private StreamOperations<String, String, String> streamOperations;
    private MutableClock clock;
    private MeterRegistry meterRegistry;
    private BroadcastStream stream;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        streamOperations = mock(StreamOperations.class);
        when(redisTemplate.<String, String>opsForStream()).thenReturn(streamOperations);

        clock = new MutableClock(Instant.parse("2026-10-05T00:00:00Z"), ZoneId.of("UTC"));
        meterRegistry = new SimpleMeterRegistry();
        stream = new BroadcastStream(redisTemplate, clock, meterRegistry);
    }

    private BroadcastMessage message() {
        return new BroadcastMessage("instance-a", DESTINATION, BroadcastMessage.EVERYONE, "{}");
    }

    private double published() {
        return meterRegistry.counter("fungame.broadcast.published").count();
    }

    private double droppedError() {
        return meterRegistry.counter("fungame.broadcast.dropped", "reason", "error").count();
    }

    private long spreadCount() {
        return meterRegistry.get("fungame.broadcast.spread.duration").timer().count();
    }

    @Test
    @DisplayName("메시지를 스트림에 싣고 보관 기간이 지난 구간을 함께 자른다.")
    void writing_also_trims_what_fell_out_of_retention() {
        stream.write(message());

        ArgumentCaptor<MapRecord<String, String, String>> record = ArgumentCaptor.captor();
        ArgumentCaptor<XAddOptions> options = ArgumentCaptor.captor();
        verify(streamOperations).add(record.capture(), options.capture());

        assertThat(record.getValue().getValue()).containsEntry("destination", DESTINATION);
        assertThat(options.getValue().getMinId().getValue())
                .isEqualTo(clock.millis() - Duration.ofMinutes(5).toMillis() + "-0");
        assertThat(published()).isEqualTo(1);
    }

    @Test
    @DisplayName("실패하면 잃은 것으로 세고 예외를 그대로 올린다. 로그를 남길지는 부르는 쪽이 정한다.")
    void a_failed_write_is_counted_and_rethrown() {
        doThrow(new QueryTimeoutException("Redis command timed out"))
                .when(streamOperations).add(any(MapRecord.class), any(XAddOptions.class));

        assertThatThrownBy(() -> stream.write(message())).isInstanceOf(QueryTimeoutException.class);

        assertThat(droppedError()).isEqualTo(1);
        assertThat(published()).isZero();
    }

    @Test
    @DisplayName("실패한 시도도 소요 시간에 들어간다. 멈춘 Redis 의 2초가 지표에 보여야 한다.")
    void a_failed_write_is_still_timed() {
        doThrow(new QueryTimeoutException("Redis command timed out"))
                .when(streamOperations).add(any(MapRecord.class), any(XAddOptions.class));

        assertThatThrownBy(() -> stream.write(message())).isInstanceOf(QueryTimeoutException.class);

        assertThat(spreadCount()).isEqualTo(1);
    }
}
