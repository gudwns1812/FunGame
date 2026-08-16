package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.support.config.AppTaskScheduler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Component
public class GameTimer {

    private static final Duration COUNT_DOWN_INTERVAL = Duration.ofSeconds(1);

    private final TaskScheduler taskScheduler;
    private final Map<Long, ScheduledFuture<?>> roomTasks = new ConcurrentHashMap<>();

    public GameTimer(@AppTaskScheduler TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    public void startCountDown(Long roomId, int seconds, Consumer<Integer> event) {
        stop(roomId);

        AtomicInteger remaining = new AtomicInteger(seconds);

        ScheduledFuture<?> task = taskScheduler.scheduleAtFixedRate(
                () -> event.accept(remaining.getAndDecrement()),
                COUNT_DOWN_INTERVAL);

        roomTasks.put(roomId, task);
    }

    public void startAfter(Long roomId, int seconds, Runnable event) {
        stop(roomId);

        ScheduledFuture<?> task = taskScheduler.schedule(event, Instant.now().plusSeconds(seconds));

        roomTasks.put(roomId, task);
    }

    public void stop(Long roomId) {
        ScheduledFuture<?> task = roomTasks.remove(roomId);
        if (task != null) {
            task.cancel(false);
        }
    }
}
