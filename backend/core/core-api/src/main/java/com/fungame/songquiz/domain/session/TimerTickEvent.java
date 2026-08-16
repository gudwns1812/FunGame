package com.fungame.songquiz.domain.session;

public record TimerTickEvent(
        Long roomId,
        int remainingSeconds
) {
}
