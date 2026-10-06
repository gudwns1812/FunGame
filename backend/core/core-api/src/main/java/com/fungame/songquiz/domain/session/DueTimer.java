package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.storage.redis.ClaimedTimer;
import java.time.Duration;

public record DueTimer(GameTimerTask task, Duration lateness, ClaimedTimer claimed) {
}
