package com.fungame.songquiz.domain.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class GameTimerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long OTHER_ROOM_ID = 2L;
    private static final Duration SOON = Duration.ofMillis(50);
    private static final Duration LATER = Duration.ofMillis(150);
    private static final Duration NEVER_WITHIN_TEST = Duration.ofSeconds(30);

    private final GameTimer gameTimer = new GameTimer(taskScheduler());
    private final List<String> fired = new CopyOnWriteArrayList<>();

    private static ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.initialize();
        return scheduler;
    }

    @Test
    @DisplayName("한 방에 예약을 둘 걸면 둘 다 살아서 각자의 시점에 터진다.")
    void keeps_every_reservation_of_a_room() {
        gameTimer.startAfter(ROOM_ID, SOON, () -> fired.add("힌트"));
        gameTimer.startAfter(ROOM_ID, LATER, () -> fired.add("타임아웃"));

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(fired).containsExactly("힌트", "타임아웃"));
    }

    @Test
    @DisplayName("stop 은 그 방의 예약을 전부 취소한다.")
    void stop_cancels_every_reservation_of_a_room() {
        gameTimer.startAfter(ROOM_ID, SOON, () -> fired.add("힌트"));
        gameTimer.startAfter(ROOM_ID, LATER, () -> fired.add("타임아웃"));

        gameTimer.stop(ROOM_ID);

        await().pollDelay(LATER.multipliedBy(3)).atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(fired).isEmpty());
    }

    @Test
    @DisplayName("한 방을 멈춰도 다른 방의 예약은 그대로 터진다.")
    void stop_leaves_other_rooms_alone() {
        gameTimer.startAfter(ROOM_ID, SOON, () -> fired.add("멈춘 방"));
        gameTimer.startAfter(OTHER_ROOM_ID, SOON, () -> fired.add("남은 방"));

        gameTimer.stop(ROOM_ID);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(fired).containsExactly("남은 방"));
    }

    @Test
    @DisplayName("이미 터진 예약은 다음 예약을 걸 때 걷어내 방마다 쌓이지 않는다.")
    void done_reservations_are_swept_away() {
        gameTimer.startAfter(ROOM_ID, SOON, () -> fired.add("지난 라운드"));
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> assertThat(fired).hasSize(1));

        gameTimer.startAfter(ROOM_ID, NEVER_WITHIN_TEST, () -> fired.add("다음 라운드"));

        assertThat(reservationsOf(ROOM_ID)).hasSize(1);
    }

    @SuppressWarnings("unchecked")
    private Collection<ScheduledFuture<?>> reservationsOf(Long roomId) {
        Map<Long, Collection<ScheduledFuture<?>>> roomTasks =
                (Map<Long, Collection<ScheduledFuture<?>>>) ReflectionTestUtils.getField(gameTimer, "roomTasks");

        return roomTasks.get(roomId);
    }
}
