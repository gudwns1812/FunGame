package com.fungame.songquiz.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import com.fungame.songquiz.storage.redis.MemberPresenceDao;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;

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

    @Test
    @DisplayName("Redis 에 남은 접속 상태를 지운다. 앞 테스트의 접속자가 다음 테스트의 접속자 목록에 섞이지 않는다.")
    void it_clears_presence_left_in_redis() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        Set<String> presenceKeys = Set.of(MemberPresenceDao.KEY_PREFIX + "online");
        given(redisTemplate.keys(MemberPresenceDao.KEY_PREFIX + "*")).willReturn(presenceKeys);

        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(StringRedisTemplate.class, () -> redisTemplate);
            context.refresh();

            new SharedStateCleaner(context).clean();
        }

        verify(redisTemplate).delete(presenceKeys);
    }
}
