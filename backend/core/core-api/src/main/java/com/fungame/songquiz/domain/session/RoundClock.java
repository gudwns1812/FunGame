package com.fungame.songquiz.domain.session;

import java.time.Duration;
import java.time.Instant;

public class RoundClock {

    private static final Duration LENGTH = Duration.ofSeconds(30);
    private static final Duration REMAINING_WHEN_HINT_OPENS = Duration.ofSeconds(10);
    private static final long NO_TIME_LEFT = 0L;

    private volatile Instant startedAt;

    public void start() {
        startedAt = Instant.now();
    }

    public void stop() {
        startedAt = null;
    }

    public Duration length() {
        return LENGTH;
    }

    public Duration untilHintOpens() {
        return LENGTH.minus(REMAINING_WHEN_HINT_OPENS);
    }

    public long remainingMillis() {
        Instant roundStartedAt = startedAt;
        if (roundStartedAt == null) {
            return NO_TIME_LEFT;
        }

        long elapsed = Duration.between(roundStartedAt, Instant.now()).toMillis();
        return Math.max(LENGTH.toMillis() - elapsed, NO_TIME_LEFT);
    }
}
