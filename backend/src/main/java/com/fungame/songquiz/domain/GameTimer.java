package com.fungame.songquiz.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameTimer {

    private final ScheduledExecutorService scheduler;
    private final Map<Long, ScheduledFuture<?>> roomTasks = new ConcurrentHashMap<>();

    public void startCountDown(Long roomId, int seconds, Consumer<Integer> event) {
        stop(roomId);

        AtomicInteger remaining = new AtomicInteger(seconds);

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            int current = remaining.getAndDecrement();

            event.accept(current);

        }, 0, 1, TimeUnit.SECONDS);

        roomTasks.put(roomId, task);
    }

    public void startAfter(Long roomId, int seconds, Runnable event) {
        stop(roomId);

        ScheduledFuture<?> task = scheduler.schedule(event, seconds, TimeUnit.SECONDS);

        roomTasks.put(roomId, task);
    }

    public void stop(Long roomId) {
        ScheduledFuture<?> task = roomTasks.remove(roomId);
        if (task != null) {
            task.cancel(false);
        }
    }
}
