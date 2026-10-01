package com.fungame.songquiz.controller.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    private static final String BEAN_NAME = "applicationTaskExecutor";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AsyncConfig.class);

    @Test
    @DisplayName("@Async 는 taskExecutor 별칭으로 executor 를 찾는다.")
    void async_finds_the_executor_by_the_taskExecutor_alias() {
        contextRunner.run(context -> assertThat(context.getBeanFactory().getAliases(BEAN_NAME))
                .as("사라지면 SimpleAsyncTaskExecutor 로 조용히 폴백한다")
                .contains(AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME));
    }

    @Test
    @DisplayName("기동과 함께 만들어진다. 첫 @Async 호출을 기다리지 않는다.")
    void the_executor_is_created_eagerly() {
        contextRunner.run(context ->
                assertThat(context.getBeanFactory().containsSingleton(BEAN_NAME)).isTrue());
    }

    @Test
    @DisplayName("큐에 상한이 있고 차면 발행 스레드가 대신 실행한다.")
    void the_queue_is_bounded_and_a_full_queue_slows_the_publisher() {
        contextRunner.run(context -> {
            ThreadPoolTaskExecutor executor = context.getBean(BEAN_NAME, ThreadPoolTaskExecutor.class);

            assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(1000);
            assertThat(executor.getMaxPoolSize()).isEqualTo(8);
            assertThat(executor.getThreadNamePrefix()).isEqualTo("app-async-");
            assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                    .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
        });
    }
}
