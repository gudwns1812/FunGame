package com.fungame.songquiz.api.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamSpreader {

    private final BroadcastMessages messages;
    private final BroadcastStream stream;
    private final BroadcastCircuitBreaker breaker;

    public void spread(String destination, String user, Object payload) {
        if (!breaker.allows()) {
            return;
        }

        messages.of(destination, user, payload).ifPresent(this::write);
    }

    private void write(BroadcastMessage message) {
        try {
            stream.write(message);
            breaker.recordSuccess();
        } catch (RuntimeException e) {
            breaker.recordFailure();
            log.error("다른 인스턴스로 브로드캐스트를 전파하지 못했다: {}", message.destination(), e);
        }
    }
}
