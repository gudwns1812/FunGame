package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.storage.redis.ClaimedTimer;
import com.fungame.songquiz.storage.redis.GameTimerDao;
import com.fungame.songquiz.support.availability.TrafficGate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameTimer {

    static final Duration LEASE = Duration.ofSeconds(10);

    private static final int CLAIM_LIMIT = 50;

    private final GameTimerDao gameTimerDao;
    private final Clock clock;
    private final TrafficGate trafficGate;

    public void startAfter(Duration delay, GameTimerTask task) {
        gameTimerDao.schedule(task.key(), now().plus(delay).toEpochMilli());
    }

    public void cancel(GameTimerTask task) {
        gameTimerDao.cancel(task.key());
    }

    public void stop(Long roomId) {
        gameTimerDao.cancelStartingWith(GameTimerTask.roomKeyPrefix(roomId));
    }

    public List<DueTimer> claimDue() {
        if (!trafficGate.isAccepting()) {
            return List.of();
        }

        Instant now = now();

        return gameTimerDao.claim(now.toEpochMilli(), now.plus(LEASE).toEpochMilli(), CLAIM_LIMIT).stream()
                .map(claimed -> dueOf(claimed, now))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<DueTimer> dueOf(ClaimedTimer claimed, Instant now) {
        Optional<GameTimerTask> task = GameTimerTask.fromKey(claimed.taskKey());
        if (task.isEmpty()) {
            log.warn("예약 작업 {} 은 이 버전이 모르는 종류다. 지우지 않고 둔다", claimed.taskKey());
        }

        return task.map(known -> new DueTimer(known, latenessOf(claimed, now), claimed));
    }

    public void complete(DueTimer due) {
        gameTimerDao.complete(due.claimed());
    }

    private static Duration latenessOf(ClaimedTimer claimed, Instant now) {
        Duration lateness = Duration.ofMillis(now.toEpochMilli() - claimed.dueAtMillis());
        return lateness.isNegative() ? Duration.ZERO : lateness;
    }

    private Instant now() {
        return clock.instant();
    }
}
