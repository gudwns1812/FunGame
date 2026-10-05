package com.fungame.songquiz.api.config;

import com.fungame.songquiz.api.websocket.BroadcastStreamListener;
import com.fungame.songquiz.api.websocket.BroadcastStream;
import java.time.Duration;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

@Slf4j
@Configuration
public class BroadcastStreamConfig {

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(1);
    private static final String THREAD_NAME_PREFIX = "broadcast-";

    @Bean(destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> broadcastStreamContainer(
            RedisConnectionFactory connectionFactory,
            BroadcastStreamListener listener,
            BroadcastStream stream) {
        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(POLL_TIMEOUT)
                        .executor(Executors.newSingleThreadExecutor(
                                new CustomizableThreadFactory(THREAD_NAME_PREFIX)))
                        .errorHandler(error -> log.error("브로드캐스트 스트림 수신이 끊겼다", error))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        container.receive(
                StreamOffset.create(BroadcastStream.KEY, stream.startOffset(BroadcastStream.KEY)),
                listener);
        container.start();

        return container;
    }
}
