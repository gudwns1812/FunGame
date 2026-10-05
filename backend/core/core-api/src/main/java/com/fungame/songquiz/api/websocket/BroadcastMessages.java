package com.fungame.songquiz.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.support.config.InstanceId;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BroadcastMessages {

    private final InstanceId instanceId;
    private final ObjectMapper objectMapper;
    private final Counter droppedSerialize;

    public BroadcastMessages(InstanceId instanceId, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.instanceId = instanceId;
        this.objectMapper = objectMapper;
        this.droppedSerialize = BroadcastLoss.counter(meterRegistry, BroadcastLoss.SERIALIZE);
    }

    public Optional<BroadcastMessage> of(String destination, String user, Object payload) {
        try {
            return Optional.of(new BroadcastMessage(
                    instanceId.value(), destination, user, objectMapper.writeValueAsString(payload)));
        } catch (Exception e) {
            droppedSerialize.increment();
            log.error("브로드캐스트 payload 를 직렬화하지 못했다: {}", destination, e);

            return Optional.empty();
        }
    }
}
