package com.fungame.songquiz.controller.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.storage.IntegrationTest;
import com.fungame.songquiz.support.config.InstanceId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@IntegrationTest
class StompBroadcasterTest {

    private static final String DESTINATION = "/topic/room/7";
    private static final String OTHER_USER = "member:42";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final InstanceId sender = new InstanceId("instance-a");
    private final InstanceId receiver = new InstanceId("instance-b");

    private SimpMessagingTemplate senderStomp;
    private SimpMessagingTemplate receiverStomp;
    private StompBroadcaster senderSide;
    private BroadcastStreamListener receiverSide;
    private BroadcastStreamListener senderSideListener;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(StompBroadcaster.STREAM_KEY);

        senderStomp = mock(SimpMessagingTemplate.class);
        receiverStomp = mock(SimpMessagingTemplate.class);

        senderSide = new StompBroadcaster(senderStomp, redisTemplate, objectMapper, sender, new SimpleMeterRegistry());
        receiverSide = new BroadcastStreamListener(
                new StompBroadcaster(receiverStomp, redisTemplate, objectMapper, receiver, new SimpleMeterRegistry()),
                objectMapper, receiver, new SimpleMeterRegistry());
        senderSideListener = new BroadcastStreamListener(senderSide, objectMapper, sender, new SimpleMeterRegistry());
    }

    private List<MapRecord<String, Object, Object>> readStream() {
        return redisTemplate.opsForStream()
                .read(StreamOffset.fromStart(StompBroadcaster.STREAM_KEY));
    }

    @SuppressWarnings("unchecked")
    private MapRecord<String, String, String> onlyRecord() {
        List<MapRecord<String, Object, Object>> records = readStream();
        assertThat(records).hasSize(1);

        return (MapRecord<String, String, String>) (MapRecord<?, ?, ?>) records.get(0);
    }

    @Test
    @DisplayName("보낸 인스턴스는 자기 구독자에게 바로 주고, 스트림에도 실어 다른 인스턴스가 받게 한다.")
    void a_broadcast_reaches_subscribers_on_another_instance() {
        senderSide.send(DESTINATION, Map.of("type", "ROUND_START", "round", 3));

        verify(senderStomp).convertAndSend(eq(DESTINATION), eq(Map.of("type", "ROUND_START", "round", 3)));

        receiverSide.onMessage(onlyRecord());

        verify(receiverStomp).convertAndSend(eq(DESTINATION), eq(Map.of("type", "ROUND_START", "round", 3)));
    }

    @Test
    @DisplayName("자기가 실은 메시지는 다시 꺼내 보내지 않는다.")
    void an_instance_skips_the_message_it_published() {
        senderSide.send(DESTINATION, Map.of("type", "ROUND_END"));

        senderSideListener.onMessage(onlyRecord());

        verify(senderStomp, times(1)).convertAndSend(eq(DESTINATION), eq(Map.of("type", "ROUND_END")));
    }

    @Test
    @DisplayName("특정 사용자에게 가는 메시지도 인스턴스를 건너 전달된다.")
    void a_user_message_crosses_instances() {
        senderSide.sendToUser(OTHER_USER, "/queue/invite", Map.of("inviteId", "abc"));

        receiverSide.onMessage(onlyRecord());

        verify(receiverStomp).convertAndSendToUser(
                eq(OTHER_USER), eq("/queue/invite"), eq(Map.of("inviteId", "abc")));
    }

    @Test
    @DisplayName("보관 기간이 지난 항목은 잘려 스트림이 무한히 자라지 않는다.")
    void old_entries_are_trimmed() {
        redisTemplate.opsForStream().add(StreamRecords
                .mapBacked(Map.of("instance", "old", "destination", "x", "user", "", "payload", "{}"))
                .withStreamKey(StompBroadcaster.STREAM_KEY)
                .withId(RecordId.of("1-0")));

        senderSide.send(DESTINATION, Map.of("type", "PING"));

        assertThat(readStream())
                .singleElement()
                .satisfies(kept -> assertThat(kept.getValue()).containsEntry("instance", "instance-a"));
    }
}
