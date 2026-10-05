package com.fungame.songquiz.api.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BroadcastCircuitBreakerConfig {

    private static final String NAME = "broadcast";

    @Bean
    public CircuitBreaker broadcastCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker(NAME);
    }
}
