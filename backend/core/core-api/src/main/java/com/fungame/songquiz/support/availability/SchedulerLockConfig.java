package com.fungame.songquiz.support.availability;

import java.util.Optional;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")
public class SchedulerLockConfig {

    public static final String KEY_PREFIX = "fungame";
    public static final String LOCK_SCOPE = "schedule-lock";

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory redisConnectionFactory, TrafficGate trafficGate) {
        LockProvider onRedis = new RedisLockProvider.Builder(redisConnectionFactory)
                .keyPrefix(KEY_PREFIX)
                .environment(LOCK_SCOPE)
                .build();

        return configuration -> trafficGate.isAccepting()
                ? onRedis.lock(configuration)
                : Optional.<SimpleLock>empty();
    }
}
