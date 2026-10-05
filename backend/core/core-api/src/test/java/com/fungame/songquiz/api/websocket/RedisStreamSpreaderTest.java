package com.fungame.songquiz.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.support.config.InstanceId;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.QueryTimeoutException;

class RedisStreamSpreaderTest {

    private static final String DESTINATION = "/topic/room/7";

    private BroadcastStream stream;
    private RedisStreamSpreader spreader;

    @BeforeEach
    void setUp() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        stream = mock(BroadcastStream.class);
        spreader = new RedisStreamSpreader(
                new BroadcastMessages(new InstanceId("instance-a"), new ObjectMapper(), meterRegistry),
                new BroadcastQueue(Clock.systemUTC(), meterRegistry, 10, 60_000),
                stream);
    }

    @Test
    @DisplayName("보내는 쪽은 스트림을 기다리지 않는다. 호출 스레드에서는 큐에 넣기만 한다.")
    void spreading_does_not_touch_the_stream_on_the_calling_thread() {
        spreader.spread(DESTINATION, BroadcastMessage.EVERYONE, Map.of("type", "ROUND_START"));

        verifyNoInteractions(stream);
    }

    @Test
    @DisplayName("워커가 큐에서 꺼내야 스트림에 쓴다.")
    void the_worker_writes_what_the_queue_hands_over() throws InterruptedException {
        spreader.spread(DESTINATION, BroadcastMessage.EVERYONE, Map.of("type", "ROUND_START"));
        spreader.pumpOnce();

        ArgumentCaptor<BroadcastMessage> written = ArgumentCaptor.captor();
        verify(stream).write(written.capture());

        assertThat(written.getValue().destination()).isEqualTo(DESTINATION);
        assertThat(written.getValue().payload()).isEqualTo("{\"type\":\"ROUND_START\"}");
    }

    @Test
    @DisplayName("쓰기가 실패해도 예외가 밖으로 나가지 않고 워커는 다음 건을 계속 처리한다.")
    void a_failed_write_does_not_stop_the_worker() throws InterruptedException {
        doThrow(new QueryTimeoutException("Redis command timed out")).when(stream).write(any());

        spreader.spread(DESTINATION, BroadcastMessage.EVERYONE, Map.of());
        spreader.pumpOnce();

        spreader.spread(DESTINATION, BroadcastMessage.EVERYONE, Map.of());
        spreader.pumpOnce();

        verify(stream, times(2)).write(any());
    }
}
