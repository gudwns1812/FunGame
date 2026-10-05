package com.fungame.songquiz.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.fungame.songquiz.storage.IntegrationTest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

@IntegrationTest
class BroadcastStreamBurstTest {

    private static final String KEY = BroadcastStream.KEY + ":burst-test";
    private static final int BURST = 5;
    private static final Duration DELIVERY_PER_RECORD = Duration.ofMillis(60);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    private final List<String> delivered = new CopyOnWriteArrayList<>();

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(KEY);
        delivered.clear();
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }

        redisTemplate.delete(KEY);
    }

    private void listenFrom(String streamKey) {
        BroadcastStream stream = new BroadcastStream(
                redisTemplate, Clock.systemUTC(), new SimpleMeterRegistry());

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .executor(Executors.newSingleThreadExecutor(
                                new CustomizableThreadFactory("burst-test-")))
                        .build();

        container = StreamMessageListenerContainer.create(connectionFactory, options);
        container.receive(StreamOffset.create(streamKey, stream.startOffset(streamKey)), record -> {
            sleep(DELIVERY_PER_RECORD);
            delivered.add(record.getValue().get("destination"));
        });
        container.start();
    }

    private void add(String destination) {
        redisTemplate.opsForStream().add(StreamRecords
                .mapBacked(Map.of("instance", "a", "destination", destination, "user", "", "payload", "{}"))
                .withStreamKey(KEY));
    }

    private static void sleep(Duration duration) {
        try {
            TimeUnit.MILLISECONDS.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("한 건을 전달하는 동안 들어온 것도 빠짐없이 받는다. 라운드 전환이 몰려 오면 그 사이가 통째로 사라진다.")
    void a_burst_is_received_in_full() {
        add("/topic/seed");
        listenFrom(KEY);
        sleep(Duration.ofMillis(500));
        delivered.clear();

        for (int i = 1; i <= BURST; i++) {
            add("/topic/room/" + i);
        }

        sleep(DELIVERY_PER_RECORD.multipliedBy(BURST).plusSeconds(3));

        assertThat(delivered).containsExactly(
                "/topic/room/1", "/topic/room/2", "/topic/room/3", "/topic/room/4", "/topic/room/5");
    }

    @Test
    @DisplayName("기동 전에 쌓여 있던 것은 다시 재생하지 않는다.")
    void history_is_not_replayed_on_startup() {
        add("/topic/before");

        listenFrom(KEY);
        sleep(Duration.ofMillis(500));

        add("/topic/after");
        sleep(Duration.ofSeconds(2));

        assertThat(delivered).containsExactly("/topic/after");
    }
}
