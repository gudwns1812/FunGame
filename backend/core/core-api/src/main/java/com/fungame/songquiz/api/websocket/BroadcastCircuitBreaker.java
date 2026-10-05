package com.fungame.songquiz.api.websocket;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BroadcastCircuitBreaker {

    private static final long CLOSED = 0;

    private final Clock clock;
    private final int failureThreshold;
    private final long cooldownMillis;
    private final Counter droppedOpen;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong openedAtMillis = new AtomicLong(CLOSED);

    public BroadcastCircuitBreaker(Clock clock,
                                   MeterRegistry meterRegistry,
                                   @Value("${app.broadcast.failure-threshold:3}") int failureThreshold,
                                   @Value("${app.broadcast.cooldown-millis:5000}") long cooldownMillis) {
        this.clock = clock;
        this.failureThreshold = failureThreshold;
        this.cooldownMillis = cooldownMillis;
        this.droppedOpen = BroadcastLoss.counter(meterRegistry, BroadcastLoss.CIRCUIT_OPEN);
    }

    public boolean allows() {
        long openedAt = openedAtMillis.get();

        if (openedAt == CLOSED) {
            return true;
        }

        if (clock.millis() - openedAt < cooldownMillis) {
            droppedOpen.increment();

            return false;
        }

        return openedAtMillis.compareAndSet(openedAt, clock.millis());
    }

    public void recordSuccess() {
        consecutiveFailures.set(0);

        if (openedAtMillis.getAndSet(CLOSED) != CLOSED) {
            log.warn("Redis 가 응답해 브로드캐스트 전파를 다시 연다");
        }
    }

    public void recordFailure() {
        if (consecutiveFailures.incrementAndGet() < failureThreshold) {
            return;
        }

        if (openedAtMillis.getAndSet(clock.millis()) == CLOSED) {
            log.error("브로드캐스트 전파를 {}ms 동안 막는다. 연속 {}회 실패했다",
                    cooldownMillis, failureThreshold);
        }
    }
}
