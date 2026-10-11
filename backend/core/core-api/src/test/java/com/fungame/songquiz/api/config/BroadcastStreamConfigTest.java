package com.fungame.songquiz.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fungame.songquiz.api.websocket.BroadcastMessage;
import com.fungame.songquiz.api.websocket.BroadcastStream;
import com.fungame.songquiz.storage.IntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

@IntegrationTest
class BroadcastStreamConfigTest {

    private static final Duration LONGER_THAN_COMMAND_TIMEOUT = Duration.ofMillis(3500);
    private static final Duration UNTIL_PAUSE_ENDS = LONGER_THAN_COMMAND_TIMEOUT.plusSeconds(1);
    private static final Duration DELIVERY_WAIT = Duration.ofSeconds(10);
    private static final String PROBE_DESTINATION = "/topic/stream-probe";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private BroadcastStream stream;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    @DisplayName("Redis 가 명령 타임아웃보다 오래 멈췄다 돌아와도 스트림 수신이 이어진다. 한 번 끊기면 재시작 전까지 로비 · 채팅 · 게임 알림이 전부 멈춘다.")
    void reception_survives_a_redis_stall_longer_than_the_command_timeout() throws InterruptedException {
        assertDelivered(this::writeProbe);

        stallRedisFor(LONGER_THAN_COMMAND_TIMEOUT);
        TimeUnit.MILLISECONDS.sleep(UNTIL_PAUSE_ENDS.toMillis());

        assertDelivered(this::writeProbe);
    }

    @Test
    @DisplayName("수신이 실패하면 센다. 다시 붙어 알림은 이어져도 Redis 가 흔들렸다는 것은 지표에 남아야 한다.")
    void a_failed_read_is_counted() throws InterruptedException {
        double before = receiveFailures();

        stallRedisFor(LONGER_THAN_COMMAND_TIMEOUT);
        TimeUnit.MILLISECONDS.sleep(UNTIL_PAUSE_ENDS.toMillis());

        assertThat(receiveFailures()).isGreaterThan(before);
    }

    private void assertDelivered(Runnable send) {
        double before = received();

        send.run();

        await().atMost(DELIVERY_WAIT)
                .untilAsserted(() -> assertThat(received()).isGreaterThan(before));
    }

    private void writeProbe() {
        stream.write(new BroadcastMessage("itest-probe", PROBE_DESTINATION, BroadcastMessage.EVERYONE, "{}"));
    }

    private void stallRedisFor(Duration duration) {
        redisTemplate.execute((RedisCallback<Object>) connection ->
                connection.execute("CLIENT", "PAUSE".getBytes(), String.valueOf(duration.toMillis()).getBytes(),
                        "ALL".getBytes()));
    }

    private double received() {
        return meterRegistry.counter("fungame.broadcast.received").count();
    }

    private double receiveFailures() {
        return meterRegistry.counter("fungame.broadcast.receive.failures").count();
    }
}
