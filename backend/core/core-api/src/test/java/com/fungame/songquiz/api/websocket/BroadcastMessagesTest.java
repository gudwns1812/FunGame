package com.fungame.songquiz.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.support.config.InstanceId;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BroadcastMessagesTest {

    private static final String DESTINATION = "/topic/room/7";

    private MeterRegistry meterRegistry;
    private BroadcastMessages messages;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        messages = new BroadcastMessages(new InstanceId("instance-a"), new ObjectMapper(), meterRegistry);
    }

    @Test
    @DisplayName("보낸 인스턴스와 목적지를 싣는다. 받는 쪽이 자기 것을 걸러내려면 인스턴스가 있어야 한다.")
    void a_message_carries_the_sending_instance() {
        BroadcastMessage message = messages.of(DESTINATION, BroadcastMessage.EVERYONE, Map.of("type", "PING"))
                .orElseThrow();

        assertThat(message.instanceId()).isEqualTo("instance-a");
        assertThat(message.destination()).isEqualTo(DESTINATION);
        assertThat(message.payload()).isEqualTo("{\"type\":\"PING\"}");
        assertThat(message.isForEveryone()).isTrue();
    }

    @Test
    @DisplayName("직렬화하지 못하면 빈 결과를 주고 잃은 것으로 센다. 조용히 사라지면 안 된다.")
    void a_payload_that_cannot_be_serialized_is_counted() {
        assertThat(messages.of(DESTINATION, BroadcastMessage.EVERYONE, new Object())).isEmpty();

        assertThat(meterRegistry.counter("fungame.broadcast.dropped", "reason", "serialize").count())
                .isEqualTo(1);
    }
}
