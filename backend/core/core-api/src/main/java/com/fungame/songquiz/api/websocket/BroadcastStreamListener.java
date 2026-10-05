package com.fungame.songquiz.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BroadcastStreamListener implements StreamListener<String, MapRecord<String, String, String>> {

    private final StompBroadcaster broadcaster;
    private final ObjectMapper objectMapper;
    private final Counter received;

    public BroadcastStreamListener(StompBroadcaster broadcaster,
                                   ObjectMapper objectMapper,
                                   MeterRegistry meterRegistry) {
        this.broadcaster = broadcaster;
        this.objectMapper = objectMapper;
        this.received = Counter.builder("fungame.broadcast.received")
                .description("스트림에서 꺼내 내 구독자에게 전달한 브로드캐스트 수")
                .register(meterRegistry);
    }

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        BroadcastMessage message = BroadcastMessage.from(record.getValue());

        try {
            broadcaster.deliverLocally(message, objectMapper.readValue(message.payload(), Object.class));

            received.increment();
        } catch (Exception e) {
            log.error("다른 인스턴스가 보낸 브로드캐스트를 전달하지 못했다: {}", message.destination(), e);
        }
    }
}
