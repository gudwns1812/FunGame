package com.fungame.songquiz.storage.redis;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

@TestConfiguration(proxyBeanMethods = false)
public class RedisTestContainer {

    private static final String IMAGE = "redis:7-alpine";
    private static final int PORT = 6379;

    @Bean
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redisContainer() {
        return new GenericContainer<>(IMAGE)
                .withExposedPorts(PORT)
                .withReuse(true);
    }
}
