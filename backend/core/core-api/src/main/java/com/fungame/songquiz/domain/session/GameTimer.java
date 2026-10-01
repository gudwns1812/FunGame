package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.support.config.GameTaskScheduler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class GameTimer {

    private final TaskScheduler taskScheduler;
    private final Timer lateness;
    private final Timer taskDuration;
    private final Map<Long, Collection<ScheduledFuture<?>>> roomTasks = new ConcurrentHashMap<>();

    public GameTimer(@GameTaskScheduler TaskScheduler taskScheduler, MeterRegistry meterRegistry) {
        this.taskScheduler = taskScheduler;
        this.lateness = Timer.builder("fungame.game.timer.lateness")
                .description("예약 시각보다 늦게 시작한 정도. 게임 스케줄러 풀이 밀리면 늘어난다")
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(5))
                .maximumExpectedValue(Duration.ofSeconds(2))
                .register(meterRegistry);
        this.taskDuration = Timer.builder("fungame.game.timer.task")
                .description("예약 작업이 도는 데 걸린 시간")
                .register(meterRegistry);
    }

    public void startAfter(Long roomId, Duration delay, Runnable event) {
        Instant dueAt = Instant.now().plus(delay);

        Collection<ScheduledFuture<?>> tasks = tasksOf(roomId);
        tasks.removeIf(Future::isDone);
        tasks.add(taskScheduler.schedule(measured(roomId, dueAt, event), dueAt));
    }

    private Runnable measured(Long roomId, Instant dueAt, Runnable event) {
        return () -> {
            lateness.record(notBefore(Duration.between(dueAt, Instant.now())));
            taskDuration.record(() -> reporting(roomId, event));
        };
    }

    private static Duration notBefore(Duration elapsed) {
        return elapsed.isNegative() ? Duration.ZERO : elapsed;
    }

    private void reporting(Long roomId, Runnable event) {
        try {
            event.run();
        } catch (Exception e) {
            log.error("방 {} 의 예약 작업이 실패했다. 이 방의 진행이 여기서 멈춘다", roomId, e);
        }
    }

    public void stop(Long roomId) {
        Collection<ScheduledFuture<?>> tasks = roomTasks.remove(roomId);
        if (tasks == null) {
            return;
        }

        tasks.forEach(task -> task.cancel(false));
    }

    private Collection<ScheduledFuture<?>> tasksOf(Long roomId) {
        return roomTasks.computeIfAbsent(roomId, id -> new ConcurrentLinkedQueue<>());
    }
}
