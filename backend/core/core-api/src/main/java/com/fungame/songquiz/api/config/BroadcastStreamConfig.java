package com.fungame.songquiz.api.config;

import com.fungame.songquiz.api.websocket.BroadcastStreamListener;
import com.fungame.songquiz.api.websocket.BroadcastStream;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamReadRequest;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.ErrorHandler;

@Slf4j
@Configuration
public class BroadcastStreamConfig {

    private static final Duration POLL_TIMEOUT = Duration.ofMillis(500);
    private static final Duration RETRY_BACKOFF = Duration.ofSeconds(1);
    private static final String THREAD_NAME_PREFIX = "broadcast-";
    private static final String RECEIVE_FAILURES_METER = "fungame.broadcast.receive.failures";

    @Bean(destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> broadcastStreamContainer(
            RedisConnectionFactory connectionFactory,
            BroadcastStreamListener listener,
            BroadcastStream stream,
            MeterRegistry meterRegistry) {
        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(POLL_TIMEOUT)
                        .executor(Executors.newSingleThreadExecutor(
                                new CustomizableThreadFactory(THREAD_NAME_PREFIX)))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        StreamOffset<String> fromLatest =
                StreamOffset.create(BroadcastStream.KEY, stream.startOffset(BroadcastStream.KEY));

        container.register(
                StreamReadRequest.builder(fromLatest)
                        .cancelOnError(error -> false)
                        .errorHandler(retryLater(receiveFailures(meterRegistry)))
                        .build(),
                listener);
        container.start();

        return container;
    }

    private static ErrorHandler retryLater(Counter receiveFailures) {
        return error -> {
            receiveFailures.increment();
            log.error("브로드캐스트 스트림을 읽지 못했다. {} 뒤 마지막으로 읽은 곳부터 다시 읽는다", RETRY_BACKOFF, error);
            pause();
        };
    }

    private static void pause() {
        try {
            TimeUnit.MILLISECONDS.sleep(RETRY_BACKOFF.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Counter receiveFailures(MeterRegistry meterRegistry) {
        return Counter.builder(RECEIVE_FAILURES_METER)
                .description("브로드캐스트 스트림 읽기에 실패한 횟수. 실패해도 다시 읽으므로 잃은 수가 아니다")
                .register(meterRegistry);
    }
}
