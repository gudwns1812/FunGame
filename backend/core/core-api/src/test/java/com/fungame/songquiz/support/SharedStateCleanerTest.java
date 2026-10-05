package com.fungame.songquiz.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

class SharedStateCleanerTest {

    @Test
    @DisplayName("열린 서킷 브레이커를 닫는다. 한 테스트가 연 브레이커가 다음 테스트의 전파를 막지 않는다.")
    void it_closes_open_circuit_breakers() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker breaker = registry.circuitBreaker("broadcast");
        breaker.transitionToOpenState();

        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(CircuitBreakerRegistry.class, () -> registry);
            context.refresh();

            new SharedStateCleaner(context).clean();
        }

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
