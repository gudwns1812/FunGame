package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.support.config.GameTaskScheduler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GameTimerPoller {

    private static final long POLL_INTERVAL_MS = 200;

    private final GameTimer gameTimer;
    private final Map<GameTimerTask.Kind, GameTimerHandler> handlerByKind;
    private final Executor executor;
    private final Timer lateness;
    private final Timer taskDuration;

    public GameTimerPoller(GameTimer gameTimer,
                           List<GameTimerHandler> handlers,
                           @GameTaskScheduler Executor executor,
                           MeterRegistry meterRegistry) {
        this.gameTimer = gameTimer;
        this.handlerByKind = handlers.stream()
                .flatMap(handler -> handler.timerKinds().stream().map(kind -> Map.entry(kind, handler)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.executor = executor;
        this.lateness = Timer.builder("fungame.game.timer.lateness")
                .description("예약 시각보다 늦게 시작한 정도. 폴링 주기와 게임 스케줄러 풀이 밀리면 늘어난다")
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(5))
                .maximumExpectedValue(Duration.ofSeconds(2))
                .register(meterRegistry);
        this.taskDuration = Timer.builder("fungame.game.timer.task")
                .description("예약 작업이 도는 데 걸린 시간")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelay = POLL_INTERVAL_MS)
    public void poll() {
        gameTimer.claimDue().forEach(due -> executor.execute(() -> run(due)));
    }

    private void run(DueTimer due) {
        GameTimerHandler handler = handlerByKind.get(due.task().kind());
        if (handler == null) {
            log.warn("{} 를 처리할 곳이 이 서버에 없다. 지우지 않고 둔다", due.task());
            return;
        }

        lateness.record(due.lateness());
        try {
            taskDuration.record(() -> handle(handler, due.task()));
        } finally {
            gameTimer.complete(due);
        }
    }

    private void handle(GameTimerHandler handler, GameTimerTask task) {
        try {
            handler.onTimer(task);
        } catch (Exception e) {
            log.error("{} 대상 {} 의 예약 작업 {} 이 실패했다. 이 진행이 여기서 멈출 수 있다",
                    task.kind(), task.targetId(), task.key(), e);
        }
    }
}
