package com.fungame.songquiz.support.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class LogContextTaskDecoratorTest {

    private final LogContextTaskDecorator decorator = new LogContextTaskDecorator();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("작업을 맡긴 스레드의 문맥을 실행하는 스레드로 옮긴다.")
    void carries_the_submitter_context_to_the_worker() throws Exception {
        MDC.put(LogContext.REQUEST_ID, "ab12cd34");
        MDC.put(LogContext.MEMBER_ID, "17");
        Map<String, String> seen = new HashMap<>();
        Runnable task = decorator.decorate(() -> seen.putAll(MDC.getCopyOfContextMap()));

        CompletableFuture.runAsync(task).get(1, TimeUnit.SECONDS);

        assertThat(seen).containsEntry(LogContext.REQUEST_ID, "ab12cd34").containsEntry(LogContext.MEMBER_ID, "17");
    }

    @Test
    @DisplayName("작업이 끝나면 실행한 스레드의 원래 문맥으로 돌아간다. 큐가 차 맡긴 스레드가 직접 돌려도 마찬가지다.")
    void restores_the_runner_context_afterwards() {
        MDC.put(LogContext.ROOM_ID, "1");
        Runnable task = decorator.decorate(() -> MDC.put(LogContext.TIMER_TASK, "room:1:END_ROUND:1"));
        MDC.put(LogContext.ROOM_ID, "2");

        task.run();

        assertThat(MDC.getCopyOfContextMap()).containsExactly(Map.entry(LogContext.ROOM_ID, "2"));
    }

    @Test
    @DisplayName("맡긴 스레드에 문맥이 없으면 실행하는 스레드에 남아 있던 문맥도 쓰지 않는다.")
    void runs_without_context_when_none_was_submitted() {
        Map<String, String> seen = new HashMap<>();
        Runnable task = decorator.decorate(() -> {
            Map<String, String> context = MDC.getCopyOfContextMap();
            if (context != null) {
                seen.putAll(context);
            }
        });
        MDC.put(LogContext.ROOM_ID, "stale");

        task.run();

        assertThat(seen).isEmpty();
        assertThat(MDC.get(LogContext.ROOM_ID)).isEqualTo("stale");
    }
}
