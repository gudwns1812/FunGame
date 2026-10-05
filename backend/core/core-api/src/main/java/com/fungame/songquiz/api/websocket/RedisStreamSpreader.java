package com.fungame.songquiz.api.websocket;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisStreamSpreader {

    public static final String CIRCUIT_BREAKER_NAME = "broadcast";

    private final BroadcastMessages messages;
    private final BroadcastStream stream;
    private final CircuitBreaker breaker;
    private final Counter droppedOpen;

    @Autowired
    public RedisStreamSpreader(BroadcastMessages messages,
                               BroadcastStream stream,
                               CircuitBreakerRegistry circuitBreakerRegistry,
                               MeterRegistry meterRegistry) {
        this(messages, stream, circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME), meterRegistry);
    }

    RedisStreamSpreader(BroadcastMessages messages,
                        BroadcastStream stream,
                        CircuitBreaker breaker,
                        MeterRegistry meterRegistry) {
        this.messages = messages;
        this.stream = stream;
        this.breaker = breaker;
        this.droppedOpen = BroadcastLoss.counter(meterRegistry, BroadcastLoss.CIRCUIT_OPEN);
    }

    public void spread(String destination, String user, Object payload) {
        messages.of(destination, user, payload).ifPresent(this::write);
    }

    private void write(BroadcastMessage message) {
        try {
            breaker.executeRunnable(() -> stream.write(message));
        } catch (CallNotPermittedException e) {
            droppedOpen.increment();
        } catch (RuntimeException e) {
            log.error("다른 인스턴스로 브로드캐스트를 전파하지 못했다: {}", message.destination(), e);
        }
    }
}
