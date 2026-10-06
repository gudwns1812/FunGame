package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.storage.redis.ClaimedTimer;
import com.fungame.songquiz.storage.redis.GameTimerDao;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameTimer {

    static final Duration LEASE = Duration.ofSeconds(10);

    private static final int CLAIM_LIMIT = 50;

    private final GameTimerDao gameTimerDao;
    private final Clock clock;

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
        Instant now = now();

        return gameTimerDao.claim(now.toEpochMilli(), now.plus(LEASE).toEpochMilli(), CLAIM_LIMIT).stream()
                .map(claimed -> new DueTimer(
                        GameTimerTask.fromKey(claimed.taskKey()),
                        latenessOf(claimed, now),
                        claimed))
                .toList();
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
