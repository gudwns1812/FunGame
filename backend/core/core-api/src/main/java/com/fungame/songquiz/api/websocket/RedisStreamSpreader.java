package com.fungame.songquiz.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fungame.songquiz.support.config.InstanceId;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisStreamSpreader {

    public static final String STREAM_KEY = "fungame:broadcast";

    private static final Duration RETENTION = Duration.ofMinutes(5);
    private static final String WORKER_THREAD_NAME = "broadcast-pump";
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(1);

    private static final String QUEUE_FULL = "queue_full";
    private static final String STALE = "stale";
    private static final String ERROR = "error";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final InstanceId instanceId;
    private final Clock clock;
    private final BlockingDeque<Pending> queue;
    private final long maxAgeMillis;

    private final Counter published;
    private final Counter droppedQueueFull;
    private final Counter droppedStale;
    private final Counter droppedError;
    private final Timer spreadDuration;

    private volatile boolean running;
    private Thread worker;

    public RedisStreamSpreader(StringRedisTemplate redisTemplate,
                               ObjectMapper objectMapper,
                               InstanceId instanceId,
                               Clock clock,
                               MeterRegistry meterRegistry,
                               @Value("${app.broadcast.queue-capacity:1000}") int queueCapacity,
                               @Value("${app.broadcast.max-age-millis:500}") long maxAgeMillis) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.instanceId = instanceId;
        this.clock = clock;
        this.queue = new LinkedBlockingDeque<>(queueCapacity);
        this.maxAgeMillis = maxAgeMillis;

        this.published = Counter.builder("fungame.broadcast.published")
                .description("다른 인스턴스로 내보낸 브로드캐스트 수")
                .register(meterRegistry);
        this.droppedQueueFull = dropped(meterRegistry, QUEUE_FULL);
        this.droppedStale = dropped(meterRegistry, STALE);
        this.droppedError = dropped(meterRegistry, ERROR);
        this.spreadDuration = Timer.builder("fungame.broadcast.spread.duration")
                .description("XADD 에 걸린 시간. 실패한 시도도 포함한다")
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(3))
                .register(meterRegistry);

        Gauge.builder("fungame.broadcast.queue.size", queue, BlockingDeque::size)
                .description("아직 내보내지 못하고 쌓여 있는 브로드캐스트 수")
                .register(meterRegistry);
    }

    private static Counter dropped(MeterRegistry meterRegistry, String reason) {
        return Counter.builder("fungame.broadcast.dropped")
                .tag("reason", reason)
                .description("다른 인스턴스로 내보내지 못한 브로드캐스트 수")
                .register(meterRegistry);
    }

    public void spread(String destination, String user, Object payload) {
        try {
            BroadcastMessage message = new BroadcastMessage(
                    instanceId.value(), destination, user, objectMapper.writeValueAsString(payload));

            enqueue(new Pending(message, clock.millis()));
        } catch (Exception e) {
            droppedError.increment();
            log.error("브로드캐스트를 큐에 넣지 못했다: {}", destination, e);
        }
    }

    private void enqueue(Pending pending) {
        if (queue.offerLast(pending)) {
            return;
        }

        if (queue.pollFirst() != null) {
            droppedQueueFull.increment();
        }

        if (!queue.offerLast(pending)) {
            droppedQueueFull.increment();
        }
    }

    void pumpOnce() throws InterruptedException {
        Pending pending = queue.pollFirst(POLL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        if (pending == null) {
            return;
        }

        if (clock.millis() - pending.enqueuedAtMillis() > maxAgeMillis) {
            droppedStale.increment();
            return;
        }

        long startedAt = System.nanoTime();
        try {
            add(pending.message());

            published.increment();
        } catch (Exception e) {
            droppedError.increment();

            if (running) {
                log.error("다른 인스턴스로 브로드캐스트를 전파하지 못했다: {}", pending.message().destination(), e);
            }
        } finally {
            spreadDuration.record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
        }
    }

    private void add(BroadcastMessage message) {
        redisTemplate.opsForStream()
                .add(StreamRecords.mapBacked(message.toFields()).withStreamKey(STREAM_KEY), retention());
    }

    private XAddOptions retention() {
        long cutoff = clock.millis() - RETENTION.toMillis();

        return XAddOptions.none().minId(RecordId.of(cutoff + "-0"));
    }

    @PostConstruct
    void startWorker() {
        running = true;
        worker = new Thread(this::pump, WORKER_THREAD_NAME);
        worker.start();
    }

    private void pump() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                pumpOnce();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @PreDestroy
    void stopWorker() {
        running = false;

        if (worker != null) {
            worker.interrupt();
        }
    }

    private record Pending(BroadcastMessage message, long enqueuedAtMillis) {
    }
}
