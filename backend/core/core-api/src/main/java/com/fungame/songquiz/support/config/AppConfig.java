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

    /**
     * 게임 라운드 전용 스케줄러. 힌트와 라운드 종료 예약만 여기서 돈다.
     * <p>
     * 라운드 종료는 예약 콜백 안에서 인라인으로 처리되고, {@code @Async} 가 붙지 않은
     * 리스너도 그 스레드에서 그대로 돈다. 한 방의 종료가 느려지면 같은 풀을 쓰는 다른 방의
     * 라운드 전환이 밀리므로, 동시에 진행되는 방 수를 기준으로 잡는다.
     */
    @Bean
    @GameTaskScheduler
    public TaskScheduler gameTaskScheduler(@Value("${app.scheduler.game.pool-size:8}") int poolSize) {
        return scheduler(poolSize, "game-timer-");
    }

    /**
     * 그 밖의 주기 작업용 스케줄러. STOMP 하트비트, 퇴장 유예, 그리고 모든 {@code @Scheduled} 가 쓴다.
     * <p>
     * <b>빈 이름이 {@code taskScheduler} 여야 한다.</b> {@code TaskScheduler} 빈이 여럿이면
     * {@code @Scheduled} 는 이름으로 찾고, 못 찾으면 단일 스레드 기본 스케줄러로 조용히 폴백한다.
     * ({@code SchedulingConfigTest} 가 이걸 지킨다)
     * <p>
     * 게임 스케줄러와 나눈 이유는 지연 특성이 다르기 때문이다. 하트비트가 밀리면 멀쩡한
     * 연결이 끊기고, 유튜브 스크래핑({@code SongScrapeScheduler})은 외부 호출이라 느릴 수 있다.
     * 그 둘이 게임 진행을 밀어내지 않게 한다.
     */
    @Bean
    @AppTaskScheduler
    public TaskScheduler taskScheduler(@Value("${app.scheduler.pool-size:5}") int poolSize) {
        return scheduler(poolSize, "app-sched-");
    }

    private static TaskScheduler scheduler(int poolSize, String threadNamePrefix) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        // 어느 풀에서 도는 작업인지 로그와 스레드 덤프에서 구분할 수 있게 한다.
        scheduler.setThreadNamePrefix(threadNamePrefix);
        scheduler.initialize();

        return scheduler;
    }
}
