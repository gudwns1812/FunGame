package com.fungame.songquiz.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import com.fungame.songquiz.domain.member.MemberConnectionTracker;
import com.fungame.songquiz.storage.redis.GameRoomDao;
import com.fungame.songquiz.storage.redis.MemberPresenceDao;
import com.fungame.songquiz.storage.redis.RoomInviteDao;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
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
    @DisplayName("Redis 에 남은 접속 상태 · 초대 · 방을 지운다. 앞 테스트의 것이 다음 테스트에 섞이지 않는다.")
    void it_clears_shared_state_left_in_redis() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        Set<String> presenceKeys = Set.of(MemberPresenceDao.KEY_PREFIX + "online");
        Set<String> inviteKeys = Set.of(RoomInviteDao.KEY_PREFIX + "2:invite-id");
        Set<String> roomKeys = Set.of(GameRoomDao.KEY_PREFIX + "7");
        given(redisTemplate.keys(MemberPresenceDao.KEY_PREFIX + "*")).willReturn(presenceKeys);
        given(redisTemplate.keys(RoomInviteDao.KEY_PREFIX + "*")).willReturn(inviteKeys);
        given(redisTemplate.keys(GameRoomDao.KEY_PREFIX + "*")).willReturn(roomKeys);

        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(StringRedisTemplate.class, () -> redisTemplate);
            context.refresh();

            new SharedStateCleaner(context).clean();
        }

        verify(redisTemplate).delete(presenceKeys);
        verify(redisTemplate).delete(inviteKeys);
        verify(redisTemplate).delete(roomKeys);
    }

    @Test
    @DisplayName("Redis 를 비운 뒤 서버 생존 신호를 다시 건다. 지운 채로 두면 다음 생존 신호까지 모든 연결이 죽은 것으로 보인다.")
    void it_restarts_presence_after_clearing_redis() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        MemberConnectionTracker tracker = mock(MemberConnectionTracker.class);

        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(StringRedisTemplate.class, () -> redisTemplate);
            context.registerBean(MemberConnectionTracker.class, () -> tracker);
            context.refresh();

            new SharedStateCleaner(context).clean();
        }

        InOrder inOrder = inOrder(redisTemplate, tracker);
        inOrder.verify(redisTemplate).keys(MemberPresenceDao.KEY_PREFIX + "*");
        inOrder.verify(tracker).start();
    }
}
