package com.fungame.songquiz.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.support.MutableClock;
import com.fungame.songquiz.support.config.InstanceId;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisStreamSpreaderTest {

    private static final String DESTINATION = "/topic/room/7";
    private static final long MAX_AGE_MILLIS = 500;

    private StringRedisTemplate redisTemplate;
    private StreamOperations<String, String, String> streamOperations;
    private MutableClock clock;
    private MeterRegistry meterRegistry;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        streamOperations = mock(StreamOperations.class);
        when(redisTemplate.<String, String>opsForStream()).thenReturn(streamOperations);

        clock = new MutableClock(Instant.parse("2026-10-05T00:00:00Z"), ZoneId.of("UTC"));
        meterRegistry = new SimpleMeterRegistry();
    }

    private RedisStreamSpreader spreader(int queueCapacity) {
        return new RedisStreamSpreader(redisTemplate, new ObjectMapper(), new InstanceId("instance-a"),
                clock, meterRegistry, queueCapacity, MAX_AGE_MILLIS);
    }

    private double dropped(String reason) {
        return meterRegistry.counter("fungame.broadcast.dropped", "reason", reason).count();
    }

    private double published() {
        return meterRegistry.counter("fungame.broadcast.published").count();
    }

    private List<String> spreadDestinations() {
        ArgumentCaptor<MapRecord<String, String, String>> captor = ArgumentCaptor.captor();
        verify(streamOperations, atLeastOnce()).add(captor.capture(), any(XAddOptions.class));

        return captor.getAllValues().stream()
                .map(record -> record.getValue().get("destination"))
                .toList();
    }

    @Test
    @DisplayName("보내는 쪽은 Redis 를 기다리지 않는다. 호출 스레드에서는 큐에 넣기만 한다.")
    void spreading_does_not_touch_redis_on_the_calling_thread() {
        spreader(10).spread(DESTINATION, BroadcastMessage.EVERYONE, Map.of("type", "ROUND_START"));

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("워커가 큐에서 꺼내야 스트림에 실린다.")
    void the_worker_puts_it_on_the_stream() throws InterruptedException {
        RedisStreamSpreader spreader = spreader(10);

        spreader.spread(DESTINATION, BroadcastMessage.EVERYONE, Map.of("type", "ROUND_START"));
        spreader.pumpOnce();

        assertThat(spreadDestinations()).containsExactly(DESTINATION);
        assertThat(published()).isEqualTo(1);
    }

    @Test
    @DisplayName("큐가 가득 차면 가장 오래된 것부터 버린다. payload 가 전체 스냅샷이라 새것이 옛것을 덮는다.")
    void a_full_queue_drops_the_oldest() throws InterruptedException {
        RedisStreamSpreader spreader = spreader(2);

        spreader.spread("/topic/first", BroadcastMessage.EVERYONE, Map.of());
        spreader.spread("/topic/second", BroadcastMessage.EVERYONE, Map.of());
        spreader.spread("/topic/third", BroadcastMessage.EVERYONE, Map.of());

        spreader.pumpOnce();
        spreader.pumpOnce();

        assertThat(spreadDestinations()).containsExactly("/topic/second", "/topic/third");
        assertThat(dropped("queue_full")).isEqualTo(1);
    }

    @Test
    @DisplayName("나이 한계를 넘긴 것은 보내지 않는다. 늦게 도착한 방송은 틀린 방송이다.")
    void a_stale_broadcast_is_not_sent() throws InterruptedException {
        RedisStreamSpreader spreader = spreader(10);

        spreader.spread(DESTINATION, BroadcastMessage.EVERYONE, Map.of("type", "ROUND_START"));
        clock.plus(Duration.ofMillis(MAX_AGE_MILLIS + 1));
        spreader.pumpOnce();

        verify(streamOperations, never()).add(any(MapRecord.class), any(XAddOptions.class));
        assertThat(dropped("stale")).isEqualTo(1);
        assertThat(published()).isZero();
    }

    @Test
    @DisplayName("전파가 실패해도 예외가 밖으로 나가지 않고 워커는 다음 건을 계속 처리한다.")
    void a_failed_spread_does_not_stop_the_worker() throws InterruptedException {
        RedisStreamSpreader spreader = spreader(10);
        doThrow(new QueryTimeoutException("Redis command timed out"))
                .when(streamOperations).add(any(MapRecord.class), any(XAddOptions.class));

        spreader.spread(DESTINATION, BroadcastMessage.EVERYONE, Map.of());
        spreader.pumpOnce();

        assertThat(dropped("error")).isEqualTo(1);
        assertThat(published()).isZero();

        spreader.spread(DESTINATION, BroadcastMessage.EVERYONE, Map.of());
        spreader.pumpOnce();

        assertThat(dropped("error")).isEqualTo(2);
    }

    @Test
    @DisplayName("큐가 비어 있으면 워커는 잠깐 기다렸다 돌아온다. 영원히 묶이면 종료가 인터럽트에만 매달린다.")
    void an_empty_queue_does_not_park_the_worker_forever() {
        RedisStreamSpreader spreader = spreader(10);

        assertTimeoutPreemptively(Duration.ofSeconds(5), spreader::pumpOnce);
    }
}
