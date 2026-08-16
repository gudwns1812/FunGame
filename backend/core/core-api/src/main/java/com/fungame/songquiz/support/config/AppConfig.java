package com.fungame.songquiz.support.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.time.Clock;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

    private static final int SCHEDULER_POOL_SIZE = 10;

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    @AppTaskScheduler
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(SCHEDULER_POOL_SIZE));
    }
}
