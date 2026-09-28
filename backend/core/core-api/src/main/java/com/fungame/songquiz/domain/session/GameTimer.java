package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.support.config.GameTaskScheduler;
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
    private final Map<Long, Collection<ScheduledFuture<?>>> roomTasks = new ConcurrentHashMap<>();

    public GameTimer(@GameTaskScheduler TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    public void startAfter(Long roomId, Duration delay, Runnable event) {
        Collection<ScheduledFuture<?>> tasks = tasksOf(roomId);
        tasks.removeIf(Future::isDone);
        tasks.add(taskScheduler.schedule(reporting(roomId, event), Instant.now().plus(delay)));
    }

    /** 예외가 {@link ScheduledFuture} 안에 담긴 채 사라지지 않게 한다. */
    private Runnable reporting(Long roomId, Runnable event) {
        return () -> {
            try {
                event.run();
            } catch (Exception e) {
                log.error("방 {} 의 예약 작업이 실패했다. 이 방의 진행이 여기서 멈춘다", roomId, e);
            }
        };
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
