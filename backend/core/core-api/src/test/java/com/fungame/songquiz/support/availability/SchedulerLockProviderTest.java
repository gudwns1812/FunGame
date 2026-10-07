package com.fungame.songquiz.support.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.fungame.songquiz.storage.redis.RedisTestContainer;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@DataRedisTest
@Import(RedisTestContainer.class)
class SchedulerLockProviderTest {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void dropLocksLeftByEarlierTests() {
        redisTemplate.delete(redisTemplate.keys(
                SchedulerLockConfig.KEY_PREFIX + ":" + SchedulerLockConfig.LOCK_SCOPE + "*"));
    }

    @Test
    @DisplayName("한 인스턴스가 쥔 작업은 다른 인스턴스가 가져가지 못한다. 유튜브를 두 번 긁지 않는다.")
    void aSecondInstanceCannotTakeTheSameWork() {
        LockProvider otherInstance = lockProviderOn(new TrafficGate(true, event -> { }));

        Optional<SimpleLock> mine = lockProviderOn(new TrafficGate(true, event -> { }))
                .lock(lockOn("fillPendingSongs"));
        Optional<SimpleLock> theirs = otherInstance.lock(lockOn("fillPendingSongs"));

        assertThat(mine).isPresent();
        assertThat(theirs).isEmpty();
    }

    @Test
    @DisplayName("트래픽을 받지 않는 서버는 락을 잡지 않는다. 깔고 앉으면 트래픽을 받는 쪽이 그 주기를 통째로 건너뛴다.")
    void aServerNotTakingTrafficNeverHoldsTheLock() {
        LockProvider standby = lockProviderOn(new TrafficGate(false, event -> { }));
        LockProvider serving = lockProviderOn(new TrafficGate(true, event -> { }));

        assertThat(standby.lock(lockOn("fillPendingSongs"))).isEmpty();

        assertThat(serving.lock(lockOn("fillPendingSongs"))).isPresent();
    }

    @Test
    @DisplayName("이름이 다른 작업은 서로 막지 않는다.")
    void differentWorkDoesNotBlockEachOther() {
        LockProvider provider = lockProviderOn(new TrafficGate(true, event -> { }));

        Optional<SimpleLock> songs = provider.lock(lockOn("fillPendingSongs"));
        Optional<SimpleLock> tokens = provider.lock(lockOn("deleteExpiredTokens"));

        assertThat(songs).isPresent();
        assertThat(tokens).isPresent();
    }

    private LockProvider lockProviderOn(TrafficGate trafficGate) {
        return new SchedulerLockConfig().lockProvider(redisConnectionFactory, trafficGate);
    }

    private static LockConfiguration lockOn(String name) {
        return new LockConfiguration(Instant.now(), name, Duration.ofMinutes(5), Duration.ZERO);
    }
}
