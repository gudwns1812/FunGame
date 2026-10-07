package com.fungame.songquiz.domain.session;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fungame.songquiz.storage.redis.GameTimerDao;
import com.fungame.songquiz.storage.redis.RedisTestContainer;
import com.fungame.songquiz.support.MutableClock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
@Import({RedisTestContainer.class, GameTimerDao.class})
class GameTimerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long OTHER_ROOM_ID = 2L;
    private static final Duration SOON = Duration.ofSeconds(3);
    private static final Duration LATER = Duration.ofSeconds(20);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GameTimerDao gameTimerDao;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-06T00:00:00Z"), ZoneId.of("UTC"));
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final List<GameTimerTask> fired = new CopyOnWriteArrayList<>();
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    private GameTimer timer;
    private GameTimerPoller poller;
    private GameTimerPoller otherInstancePoller;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(redisTemplate.keys(GameTimerDao.KEY_PREFIX + "*"));
        timer = new GameTimer(gameTimerDao, clock);
        poller = pollerOn(timer, recording());
        otherInstancePoller = pollerOn(new GameTimer(gameTimerDao, clock), recording());

        logs.start();
        pollerLogger().addAppender(logs);
    }

    @AfterEach
    void stopListeningToLogs() {
        pollerLogger().detachAppender(logs);
        logs.stop();
    }

    private GameTimerPoller pollerOn(GameTimer gameTimer, GameTimerHandler handler) {
        return new GameTimerPoller(gameTimer, List.of(handler), Runnable::run, meterRegistry);
    }

    private GameTimerHandler recording() {
        return new GameTimerHandler() {
            @Override
            public List<GameTimerTask.Kind> timerKinds() {
                return Arrays.asList(GameTimerTask.Kind.values());
            }

            @Override
            public void onTimer(GameTimerTask task) {
                fired.add(task);
            }
        };
    }

    private static Logger pollerLogger() {
        return (Logger) LoggerFactory.getLogger(GameTimerPoller.class);
    }

    @Test
    @DisplayName("예약한 작업은 시각이 되기 전에는 돌지 않고, 시각이 되면 돈다.")
    void runs_a_task_when_it_is_due() {
        GameTimerTask hint = GameTimerTask.openHint(ROOM_ID, 1);
        timer.startAfter(SOON, hint);

        poller.poll();
        assertThat(fired).isEmpty();

        clock.plus(SOON);
        poller.poll();
        assertThat(fired).containsExactly(hint);
    }

    @Test
    @DisplayName("여러 서버가 함께 가져가도 작업은 한 번만 돈다.")
    void runs_a_task_once_across_instances() {
        timer.startAfter(SOON, GameTimerTask.endRound(ROOM_ID, 1));
        clock.plus(SOON);

        poller.poll();
        otherInstancePoller.poll();

        assertThat(fired).hasSize(1);
    }

    @Test
    @DisplayName("끝낸 작업은 다시 돌지 않는다.")
    void a_finished_task_does_not_run_again() {
        timer.startAfter(SOON, GameTimerTask.endRound(ROOM_ID, 1));
        clock.plus(SOON);
        poller.poll();

        clock.plus(Duration.ofMinutes(5));
        poller.poll();
        otherInstancePoller.poll();

        assertThat(fired).hasSize(1);
    }

    @Test
    @DisplayName("가져간 서버가 끝내지 못하고 죽으면, 리스가 지난 뒤 다른 서버가 다시 가져가 돌린다.")
    void another_instance_takes_over_a_task_its_runner_never_finished() {
        GameTimerTask timeout = GameTimerTask.endRound(ROOM_ID, 1);
        timer.startAfter(SOON, timeout);
        clock.plus(SOON);
        timer.claimDue();

        otherInstancePoller.poll();
        assertThat(fired).isEmpty();

        clock.plus(GameTimer.LEASE);
        otherInstancePoller.poll();
        assertThat(fired).containsExactly(timeout);
    }

    @Test
    @DisplayName("stop 은 그 방의 예약을 전부 지운다. 다른 방의 예약은 그대로 돈다.")
    void stop_cancels_only_the_room() {
        timer.startAfter(SOON, GameTimerTask.openHint(ROOM_ID, 1));
        timer.startAfter(LATER, GameTimerTask.endRound(ROOM_ID, 1));
        GameTimerTask otherRoom = GameTimerTask.endRound(OTHER_ROOM_ID, 1);
        timer.startAfter(SOON, otherRoom);

        timer.stop(ROOM_ID);
        clock.plus(LATER);
        poller.poll();

        assertThat(fired).containsExactly(otherRoom);
    }

    @Test
    @DisplayName("cancel 은 그 작업 하나만 지운다.")
    void cancel_removes_only_the_task() {
        GameTimerTask leave = GameTimerTask.leaveRoom(7L);
        timer.startAfter(SOON, leave);
        GameTimerTask hint = GameTimerTask.openHint(ROOM_ID, 1);
        timer.startAfter(SOON, hint);

        timer.cancel(leave);
        clock.plus(SOON);
        poller.poll();

        assertThat(fired).containsExactly(hint);
    }

    @Test
    @DisplayName("같은 작업을 다시 예약하면 하나만 남고 나중 시각을 따른다.")
    void rescheduling_keeps_one_task_at_the_new_time() {
        GameTimerTask leave = GameTimerTask.leaveRoom(7L);
        timer.startAfter(SOON, leave);
        timer.startAfter(LATER, leave);

        clock.plus(SOON);
        poller.poll();
        assertThat(fired).isEmpty();

        clock.plus(LATER);
        poller.poll();
        assertThat(fired).containsExactly(leave);
    }

    @Test
    @DisplayName("처리기가 터지면 어느 작업인지 로그를 남기고, 다시 돌리지 않는다.")
    void a_blown_task_is_logged_and_not_retried() {
        GameTimerPoller failing = pollerOn(timer, new GameTimerHandler() {
            @Override
            public List<GameTimerTask.Kind> timerKinds() {
                return List.of(GameTimerTask.Kind.END_ROUND);
            }

            @Override
            public void onTimer(GameTimerTask task) {
                fired.add(task);
                throw new IllegalStateException("라운드를 닫지 못했다");
            }
        });
        timer.startAfter(SOON, GameTimerTask.endRound(ROOM_ID, 1));
        clock.plus(SOON);

        failing.poll();
        clock.plus(GameTimer.LEASE.multipliedBy(2));
        failing.poll();

        assertThat(fired).hasSize(1);
        assertThat(logs.list).anySatisfy(logged -> {
            assertThat(logged.getLevel()).isEqualTo(Level.ERROR);
            assertThat(logged.getFormattedMessage()).contains(String.valueOf(ROOM_ID));
            assertThat(logged.getThrowableProxy().getMessage()).isEqualTo("라운드를 닫지 못했다");
        });
    }

    @Test
    @DisplayName("작업이 늦게 시작한 정도와 걸린 시간을 지표로 남긴다.")
    void records_lateness_and_duration() {
        timer.startAfter(SOON, GameTimerTask.endRound(ROOM_ID, 1));
        clock.plus(SOON.plusSeconds(1));

        poller.poll();

        assertThat(meterRegistry.timer("fungame.game.timer.lateness").count()).isEqualTo(1);
        assertThat(meterRegistry.timer("fungame.game.timer.lateness").max(TimeUnit.MILLISECONDS))
                .isEqualTo(1000);
        assertThat(meterRegistry.timer("fungame.game.timer.task").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("모르는 종류의 작업을 같이 가져와도 나머지 작업은 그대로 돈다.")
    void an_unknown_kind_does_not_hold_up_the_tasks_claimed_with_it() {
        GameTimerTask hint = GameTimerTask.openHint(ROOM_ID, 1);
        plantTask("room:" + OTHER_ROOM_ID + ":SOMETHING_NEWER:1");
        timer.startAfter(SOON, hint);
        clock.plus(SOON);

        poller.poll();

        assertThat(fired).containsExactly(hint);
    }

    @Test
    @DisplayName("모르는 종류의 작업은 지우지 않는다. 리스가 지나면 그 종류를 아는 서버가 가져간다.")
    void an_unknown_kind_is_left_for_a_server_that_knows_it() {
        String newerKind = "room:" + ROOM_ID + ":SOMETHING_NEWER:1";
        plantTask(newerKind);

        poller.poll();

        assertThat(isQueued(newerKind)).isTrue();
    }

    @Test
    @DisplayName("처리기가 없는 작업은 지우지 않는다. 리스가 지나면 처리기를 가진 서버가 가져가 돌린다.")
    void a_task_without_a_handler_is_left_for_a_server_that_has_one() {
        GameTimerPoller withoutEndRound = pollerOn(timer, handlerFor(GameTimerTask.Kind.START_ROUND));
        GameTimerTask endRound = GameTimerTask.endRound(ROOM_ID, 1);
        timer.startAfter(SOON, endRound);
        clock.plus(SOON);

        withoutEndRound.poll();

        assertThat(fired).isEmpty();
        assertThat(isQueued(endRound.key())).isTrue();

        clock.plus(GameTimer.LEASE.multipliedBy(2));
        poller.poll();

        assertThat(fired).containsExactly(endRound);
    }

    private GameTimerHandler handlerFor(GameTimerTask.Kind... kinds) {
        return new GameTimerHandler() {
            @Override
            public List<GameTimerTask.Kind> timerKinds() {
                return List.of(kinds);
            }

            @Override
            public void onTimer(GameTimerTask task) {
                fired.add(task);
            }
        };
    }

    private void plantTask(String taskKey) {
        redisTemplate.opsForZSet().add(dueKey(), taskKey, clock.instant().toEpochMilli());
    }

    private boolean isQueued(String taskKey) {
        return redisTemplate.opsForZSet().score(dueKey(), taskKey) != null;
    }

    private static String dueKey() {
        return GameTimerDao.KEY_PREFIX + "due";
    }
}
