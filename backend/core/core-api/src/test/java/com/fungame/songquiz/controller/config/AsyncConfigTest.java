package com.fungame.songquiz.controller.config;

import com.fungame.songquiz.support.config.AppConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    private static final String BOOT_EXECUTOR = TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PropertyPlaceholderAutoConfiguration.class,
                    TaskExecutionAutoConfiguration.class))
            .withUserConfiguration(AppConfig.class, AsyncConfig.class)
            .withPropertyValues(
                    "spring.task.execution.thread-name-prefix=app-async-",
                    "spring.task.execution.pool.core-size=4",
                    "spring.task.execution.pool.max-size=8",
                    "spring.task.execution.pool.queue-capacity=1000");

    @Test
    @DisplayName("@Async 는 타입으로 executor 를 못 찾는다. taskExecutor 별칭이 유일한 연결고리다.")
    void async_finds_the_executor_only_by_the_taskExecutor_alias() {
        contextRunner.run(context -> {
            // 게임 스케줄러 · 앱 스케줄러 · async executor 셋 다 TaskExecutor 다
            assertThat(context.getBeanNamesForType(TaskExecutor.class)).hasSizeGreaterThan(1);

            assertThat(context.getBeanFactory().getAliases(BOOT_EXECUTOR))
                    .as("이 별칭이 사라지면 Spring 이 요청마다 스레드를 새로 만드는 "
                            + "SimpleAsyncTaskExecutor 로 조용히 폴백한다")
                    .contains(AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME);
        });
    }

    @Test
    @DisplayName("@Async 큐에 상한이 있다. 부하가 오면 조용히 밀리는 대신 드러난다.")
    void async_queue_is_bounded() {
        contextRunner.run(context -> {
            ThreadPoolTaskExecutor executor = context.getBean(BOOT_EXECUTOR, ThreadPoolTaskExecutor.class);

            assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity())
                    .as("부트 기본값은 Integer.MAX_VALUE 다")
                    .isEqualTo(1000);
            assertThat(executor.getMaxPoolSize()).isEqualTo(8);
            assertThat(executor.getThreadNamePrefix()).isEqualTo("app-async-");
        });
    }

    @Test
    @DisplayName("큐가 차면 버리지 않고 발행 스레드가 대신 실행한다.")
    void a_full_queue_slows_the_publisher_instead_of_dropping_the_message() {
        contextRunner.run(context -> {
            ThreadPoolTaskExecutor executor = context.getBean(BOOT_EXECUTOR, ThreadPoolTaskExecutor.class);

            assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                    .as("여기 실리는 일은 대부분 방송이다. 버리면 클라이언트가 이벤트를 영영 못 받는다")
                    .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
        });
    }
}
