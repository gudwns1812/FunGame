package com.fungame.songquiz.controller.config;

import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /** executor 빈을 직접 정의하면 부트가 달아둔 {@code taskExecutor} 별칭이 사라져
     *  {@code @Async} 가 {@code SimpleAsyncTaskExecutor} 로 조용히 폴백한다. 거부 정책만 얹는다.
     *  큐가 차면 방송을 버리지 않고 발행 스레드가 대신 실행한다. */
    @Bean
    public ThreadPoolTaskExecutorCustomizer asyncExecutorRejectionPolicy() {
        return executor -> executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
