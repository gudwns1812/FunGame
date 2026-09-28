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

    /**
     * 예약 작업이 던진 예외는 {@link ScheduledFuture} 안에 담긴 채 끝난다. 아무도 꺼내 보지 않으므로
     * 감싸지 않으면 그 방의 진행이 로그 한 줄 없이 멈춘다. 멈추는 것 자체는 막지 못해도 어느 방인지는 남긴다.
     */
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
