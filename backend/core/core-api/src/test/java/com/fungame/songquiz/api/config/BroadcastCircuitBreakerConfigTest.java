package com.fungame.songquiz.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class BroadcastCircuitBreakerConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(CircuitBreakerAutoConfiguration.class))
            .withUserConfiguration(BroadcastCircuitBreakerConfig.class);

    @Test
    @DisplayName("application.yml 의 broadcast 설정이 브레이커에 실린다. 이름이 어긋나면 기본값으로 조용히 돈다.")
    void the_breaker_runs_on_the_broadcast_settings() {
        contextRunner.run(context -> {
            CircuitBreakerConfig config = context.getBean(CircuitBreaker.class).getCircuitBreakerConfig();

            assertThat(config.getSlowCallDurationThreshold()).isEqualTo(Duration.ofMillis(500));
            assertThat(config.getSlowCallRateThreshold()).isEqualTo(50);
            assertThat(config.getFailureRateThreshold()).isEqualTo(50);
            assertThat(config.getSlidingWindowType()).isEqualTo(SlidingWindowType.COUNT_BASED);
            assertThat(config.getSlidingWindowSize()).isEqualTo(5);
            assertThat(config.getMinimumNumberOfCalls()).isEqualTo(3);
            assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(5_000);
            assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(1);
            assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        });
    }
}
