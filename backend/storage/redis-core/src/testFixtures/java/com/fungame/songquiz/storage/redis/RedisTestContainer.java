package com.fungame.songquiz.storage.redis;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;

@TestConfiguration(proxyBeanMethods = false)
public class RedisTestContainer {

    private static final String IMAGE = "redis:7-alpine";
    private static final int PORT = 6379;

    private static final GenericContainer<?> REDIS = new GenericContainer<>(IMAGE)
            .withExposedPorts(PORT)
            .withReuse(true);

    @Bean
    static GenericContainer<?> redisContainer() {
        return REDIS;
    }

    @Bean
    DynamicPropertyRegistrar redisConnectionProperties() {
        return registry -> {
            registry.add("spring.data.redis.host", REDIS::getHost);
            registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(PORT));
        };
    }
}
