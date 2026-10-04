package com.fungame.songquiz.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fungame.songquiz.domain.session.GameTimer;
import com.fungame.songquiz.support.ApiIntegrationTest;
import com.fungame.songquiz.support.config.AppTaskScheduler;
import com.fungame.songquiz.support.config.GameTaskScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.test.util.ReflectionTestUtils;

class SchedulingConfigTest extends ApiIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    @AppTaskScheduler
    private TaskScheduler appTaskScheduler;

    @Autowired
    @GameTaskScheduler
    private TaskScheduler gameTaskScheduler;

    @Autowired
    private GameTimer gameTimer;

    @Test
    @DisplayName("@Scheduled 는 애플리케이션의 taskScheduler 빈을 사용한다.")
    void scheduledTasksUseApplicationTaskScheduler() {
        ScheduledAnnotationBeanPostProcessor processor =
                applicationContext.getBean(ScheduledAnnotationBeanPostProcessor.class);
        ScheduledTaskRegistrar registrar =
                (ScheduledTaskRegistrar) ReflectionTestUtils.getField(processor, "registrar");

        assertThat(registrar).isNotNull();

        TaskScheduler resolved = (TaskScheduler) ReflectionTestUtils.invokeMethod(
                registrar.getScheduler(), "determineDefaultScheduler");

        assertThat(resolved)
                .as("TaskScheduler 빈이 여러 개라 이름으로 해석된다. taskScheduler 라는 이름이 사라지면 "
                        + "Spring 이 단일 스레드 기본 스케줄러로 조용히 폴백한다")
                .isSameAs(appTaskScheduler);
    }

    @Test
    @DisplayName("게임 타이머는 전용 스케줄러를 쓴다. @Scheduled 나 하트비트와 풀을 나눠 쓰지 않는다.")
    void gameTimerDoesNotShareThePoolWithPeriodicTasks() {
        TaskScheduler used = (TaskScheduler) ReflectionTestUtils.getField(gameTimer, "taskScheduler");

        assertThat(used)
                .isSameAs(gameTaskScheduler)
                .isNotSameAs(appTaskScheduler);
    }
}
