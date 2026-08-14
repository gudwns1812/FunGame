package com.fungame.songquiz.domain.event;

public record TimerTickEvent(
        Long roomId,
        int remainingSeconds
) {
}
