package com.fungame.songquiz.support.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Clock;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    @GameTaskScheduler
    public TaskScheduler gameTaskScheduler(@Value("${app.scheduler.game.pool-size:8}") int poolSize) {
        return scheduler(poolSize, "game-timer-");
    }

    /** 하트비트 · 퇴장 유예 · 모든 {@code @Scheduled} 가 쓴다.
     *  빈 이름이 {@code taskScheduler} 가 아니면 {@code @Scheduled} 가 단일 스레드로 조용히 폴백한다. */
    @Bean
    @AppTaskScheduler
    public TaskScheduler taskScheduler(@Value("${app.scheduler.pool-size:5}") int poolSize) {
        return scheduler(poolSize, "app-sched-");
    }

    private static TaskScheduler scheduler(int poolSize, String threadNamePrefix) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix(threadNamePrefix);
        scheduler.initialize();

        return scheduler;
    }
}
