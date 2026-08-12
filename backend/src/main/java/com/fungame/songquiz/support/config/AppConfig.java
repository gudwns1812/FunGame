package com.fungame.songquiz.support.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(10);
    }

    @Bean
    @AppTaskScheduler
    public TaskScheduler taskScheduler(ScheduledExecutorService scheduledExecutorService) {
        return new ConcurrentTaskScheduler(scheduledExecutorService);
    }
}
