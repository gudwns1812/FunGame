package com.fungame.songquiz.api.websocket;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class BroadcastStream {

    public static final String KEY = "fungame:broadcast";

    private static final Duration RETENTION = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;
    private final Counter published;
    private final Counter droppedError;
    private final Timer writeDuration;

    public BroadcastStream(StringRedisTemplate redisTemplate, Clock clock, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
        this.published = Counter.builder("fungame.broadcast.published")
                .description("다른 인스턴스로 내보낸 브로드캐스트 수")
                .register(meterRegistry);
        this.droppedError = BroadcastLoss.counter(meterRegistry, BroadcastLoss.ERROR);
        this.writeDuration = Timer.builder("fungame.broadcast.spread.duration")
                .description("XADD 에 걸린 시간. 실패한 시도도 포함한다")
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(3))
                .register(meterRegistry);
    }

    public void write(BroadcastMessage message) {
        long startedAt = System.nanoTime();
        try {
            redisTemplate.opsForStream()
                    .add(StreamRecords.mapBacked(message.toFields()).withStreamKey(KEY), retention());

            published.increment();
        } catch (RuntimeException e) {
            droppedError.increment();

            throw e;
        } finally {
            writeDuration.record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
        }
    }

    public ReadOffset startOffset(String streamKey) {
        try {
            return ReadOffset.from(redisTemplate.opsForStream().info(streamKey).lastGeneratedId());
        } catch (Exception e) {
            return ReadOffset.from(clock.millis() + "-0");
        }
    }

    private XAddOptions retention() {
        long cutoff = clock.millis() - RETENTION.toMillis();

        return XAddOptions.none().minId(RecordId.of(cutoff + "-0"));
    }
}
