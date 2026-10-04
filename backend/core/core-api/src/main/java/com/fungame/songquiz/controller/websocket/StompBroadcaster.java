package com.fungame.songquiz.controller.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.support.config.InstanceId;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class StompBroadcaster {

    public static final String STREAM_KEY = "fungame:broadcast";

    private static final Duration RETENTION = Duration.ofMinutes(5);
    private static final String INSTANCE_FIELD = "instance";
    private static final String DESTINATION_FIELD = "destination";
    private static final String USER_FIELD = "user";
    private static final String PAYLOAD_FIELD = "payload";

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final InstanceId instanceId;
    private final Counter published;

    public StompBroadcaster(SimpMessagingTemplate messagingTemplate,
                            StringRedisTemplate redisTemplate,
                            ObjectMapper objectMapper,
                            InstanceId instanceId,
                            MeterRegistry meterRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.instanceId = instanceId;
        this.published = Counter.builder("fungame.broadcast.published")
                .description("다른 인스턴스로 내보낸 브로드캐스트 수")
                .register(meterRegistry);
    }

    public void send(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
        spread(destination, BroadcastMessage.EVERYONE, payload);
    }

    public void sendToUser(String user, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(user, destination, payload);
        spread(destination, user, payload);
    }

    public void deliverLocally(BroadcastMessage message) {
        if (message.isForEveryone()) {
            messagingTemplate.convertAndSend(message.destination(), message.payload());
            return;
        }

        messagingTemplate.convertAndSendToUser(message.user(), message.destination(), message.payload());
    }

    private void spread(String destination, String user, Object payload) {
        try {
            Map<String, String> body = Map.of(
                    INSTANCE_FIELD, instanceId.value(),
                    DESTINATION_FIELD, destination,
                    USER_FIELD, user,
                    PAYLOAD_FIELD, objectMapper.writeValueAsString(payload));

            redisTemplate.opsForStream()
                    .add(StreamRecords.mapBacked(body).withStreamKey(STREAM_KEY), retention());

            published.increment();
        } catch (Exception e) {
            log.error("다른 인스턴스로 브로드캐스트를 전파하지 못했다: {}", destination, e);
        }
    }

    private static XAddOptions retention() {
        long cutoff = System.currentTimeMillis() - RETENTION.toMillis();

        return XAddOptions.none().minId(RecordId.of(cutoff + "-0"));
    }
}
