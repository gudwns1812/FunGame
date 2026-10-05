package com.fungame.songquiz.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.fungame.songquiz.support.MutableClock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BroadcastQueueTest {

    private static final long MAX_AGE_MILLIS = 500;

    private MutableClock clock;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-05T00:00:00Z"), ZoneId.of("UTC"));
        meterRegistry = new SimpleMeterRegistry();
    }

    private BroadcastQueue queue(int capacity) {
        return new BroadcastQueue(clock, meterRegistry, capacity, MAX_AGE_MILLIS);
    }

    private BroadcastMessage message(String destination) {
        return new BroadcastMessage("instance-a", destination, BroadcastMessage.EVERYONE, "{}");
    }

    private double dropped(String reason) {
        return meterRegistry.counter("fungame.broadcast.dropped", "reason", reason).count();
    }

    @Test
    @DisplayName("넣은 순서대로 나온다. 인스턴스별 발행 순서가 유지돼야 한다.")
    void messages_come_out_in_order() throws InterruptedException {
        BroadcastQueue queue = queue(10);

        queue.offer(message("/topic/first"));
        queue.offer(message("/topic/second"));

        assertThat(queue.pollFresh().destination()).isEqualTo("/topic/first");
        assertThat(queue.pollFresh().destination()).isEqualTo("/topic/second");
    }

    @Test
    @DisplayName("가득 차면 가장 오래된 것부터 버린다. payload 가 전체 스냅샷이라 새것이 옛것을 덮는다.")
    void a_full_queue_drops_the_oldest() throws InterruptedException {
        BroadcastQueue queue = queue(2);

        queue.offer(message("/topic/first"));
        queue.offer(message("/topic/second"));
        queue.offer(message("/topic/third"));

        assertThat(queue.pollFresh().destination()).isEqualTo("/topic/second");
        assertThat(queue.pollFresh().destination()).isEqualTo("/topic/third");
        assertThat(dropped("queue_full")).isEqualTo(1);
    }

    @Test
    @DisplayName("나이 한계를 넘긴 것은 내주지 않는다. 늦게 도착한 방송은 틀린 방송이다.")
    void a_stale_message_is_not_handed_out() throws InterruptedException {
        BroadcastQueue queue = queue(10);

        queue.offer(message("/topic/first"));
        clock.plus(Duration.ofMillis(MAX_AGE_MILLIS + 1));

        assertThat(queue.pollFresh()).isNull();
        assertThat(dropped("stale")).isEqualTo(1);
    }

    @Test
    @DisplayName("비어 있으면 잠깐 기다렸다 돌아온다. 영원히 묶이면 종료가 인터럽트에만 매달린다.")
    void an_empty_queue_does_not_park_the_caller_forever() {
        BroadcastQueue queue = queue(10);

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> assertThat(queue.pollFresh()).isNull());
    }

    @Test
    @DisplayName("쌓여 있는 양을 지표로 낸다.")
    void the_backlog_is_measured() {
        BroadcastQueue queue = queue(10);

        queue.offer(message("/topic/first"));

        assertThat(meterRegistry.get("fungame.broadcast.queue.size").gauge().value()).isEqualTo(1);
    }
}
