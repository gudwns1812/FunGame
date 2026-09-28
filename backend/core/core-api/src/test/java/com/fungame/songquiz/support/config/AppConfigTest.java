package com.fungame.songquiz.support.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

    private final AppConfig appConfig = new AppConfig();

    @Test
    @DisplayName("게임 스케줄러와 그 밖의 주기 작업 스케줄러는 서로 다른 풀이다.")
    void game_and_app_schedulers_do_not_share_a_pool() {
        ThreadPoolTaskScheduler game = (ThreadPoolTaskScheduler) appConfig.gameTaskScheduler(8);
        ThreadPoolTaskScheduler app = (ThreadPoolTaskScheduler) appConfig.taskScheduler(5);

        try {
            assertThat(game).isNotSameAs(app);
            assertThat(game.getScheduledExecutor()).isNotSameAs(app.getScheduledExecutor());
            // getPoolSize() 는 실제로 만들어진 스레드 수라 작업 전에는 0 이다.
            assertThat(game.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(8);
            assertThat(app.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(5);
        } finally {
            game.destroy();
            app.destroy();
        }
    }

    @Test
    @DisplayName("스레드 이름으로 어느 풀에서 도는 작업인지 구분된다.")
    void threads_are_named_by_pool() {
        ThreadPoolTaskScheduler game = (ThreadPoolTaskScheduler) appConfig.gameTaskScheduler(1);
        ThreadPoolTaskScheduler app = (ThreadPoolTaskScheduler) appConfig.taskScheduler(1);

        try {
            assertThat(game.getThreadNamePrefix()).isEqualTo("game-timer-");
            assertThat(app.getThreadNamePrefix()).isEqualTo("app-sched-");
        } finally {
            game.destroy();
            app.destroy();
        }
    }

    @Test
    @DisplayName("풀 크기는 설정으로 바꿀 수 있다.")
    void pool_size_is_configurable() {
        TaskScheduler scheduler = appConfig.gameTaskScheduler(3);

        try {
            assertThat(((ThreadPoolTaskScheduler) scheduler)
                    .getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(3);
        } finally {
            ((ThreadPoolTaskScheduler) scheduler).destroy();
        }
    }
}
