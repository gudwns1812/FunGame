package com.fungame.songquiz.api.websocket;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BroadcastQueue {

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(1);

    private final BlockingDeque<Pending> pending;
    private final Clock clock;
    private final long maxAgeMillis;
    private final Counter droppedQueueFull;
    private final Counter droppedStale;

    public BroadcastQueue(Clock clock,
                          MeterRegistry meterRegistry,
                          @Value("${app.broadcast.queue-capacity:1000}") int capacity,
                          @Value("${app.broadcast.max-age-millis:2000}") long maxAgeMillis) {
        this.pending = new LinkedBlockingDeque<>(capacity);
        this.clock = clock;
        this.maxAgeMillis = maxAgeMillis;
        this.droppedQueueFull = BroadcastLoss.counter(meterRegistry, BroadcastLoss.QUEUE_FULL);
        this.droppedStale = BroadcastLoss.counter(meterRegistry, BroadcastLoss.STALE);

        Gauge.builder("fungame.broadcast.queue.size", pending, BlockingDeque::size)
                .description("아직 내보내지 못하고 쌓여 있는 브로드캐스트 수")
                .register(meterRegistry);
    }

    public void offer(BroadcastMessage message) {
        Pending entry = new Pending(message, clock.millis());

        if (this.pending.offerLast(entry)) {
            return;
        }

        if (this.pending.pollFirst() != null) {
            droppedQueueFull.increment();
        }

        if (!this.pending.offerLast(entry)) {
            droppedQueueFull.increment();
        }
    }

    public BroadcastMessage pollFresh() throws InterruptedException {
        Pending entry = pending.pollFirst(POLL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        if (entry == null) {
            return null;
        }

        if (clock.millis() - entry.queuedAtMillis() > maxAgeMillis) {
            droppedStale.increment();

            return null;
        }

        return entry.message();
    }

    private record Pending(BroadcastMessage message, long queuedAtMillis) {
    }
}
