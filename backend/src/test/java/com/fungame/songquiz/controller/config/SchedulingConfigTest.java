package com.fungame.songquiz.controller.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SchedulingConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

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
                .isSameAs(applicationContext.getBean("taskScheduler", TaskScheduler.class));
    }
}
