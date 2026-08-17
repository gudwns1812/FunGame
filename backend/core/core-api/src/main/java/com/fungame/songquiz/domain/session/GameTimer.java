package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.support.config.AppTaskScheduler;
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

@Component
public class GameTimer {

    private final TaskScheduler taskScheduler;
    private final Map<Long, Collection<ScheduledFuture<?>>> roomTasks = new ConcurrentHashMap<>();

    public GameTimer(@AppTaskScheduler TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    public void startAfter(Long roomId, Duration delay, Runnable event) {
        Collection<ScheduledFuture<?>> tasks = tasksOf(roomId);
        tasks.removeIf(Future::isDone);
        tasks.add(taskScheduler.schedule(event, Instant.now().plus(delay)));
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
