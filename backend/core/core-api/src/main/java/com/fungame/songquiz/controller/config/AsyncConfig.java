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

    /**
     * {@code @Async} 가 쓰는 executor 의 거부 정책만 바꾼다. 빈 자체는 부트가 만든
     * {@code applicationTaskExecutor} 를 그대로 쓴다. 크기와 큐는 {@code application.yml} 의
     * {@code spring.task.execution.*} 이 정한다.
     * <p>
     * <b>이 빈을 직접 정의하면 안 된다.</b> 부트의 자동 구성은 {@code applicationTaskExecutor} 에
     * {@code taskExecutor} 라는 별칭을 함께 단다. 이 프로젝트에는 {@code TaskExecutor} 타입 빈이
     * 셋(게임 스케줄러 · 앱 스케줄러 · 이 executor)이라 타입 조회가 실패하고, {@code @Async} 는
     * 그 별칭으로 찾아낸다. 직접 정의해서 별칭이 사라지면 Spring 이 요청마다 스레드를 새로 만드는
     * {@code SimpleAsyncTaskExecutor} 로 조용히 폴백한다. ({@code AsyncConfigTest} 가 이걸 지킨다)
     * <p>
     * 거부 정책은 {@code CallerRunsPolicy} 다. 여기 실리는 일은 대부분 방송이라, 큐가 차면
     * 버리는 것보다 발행 스레드가 대신 실행해 느려지는 편이 낫다. 버리면 클라이언트가 이벤트를
     * 영영 못 받는다.
     */
    @Bean
    public ThreadPoolTaskExecutorCustomizer asyncExecutorRejectionPolicy() {
        return executor -> executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
