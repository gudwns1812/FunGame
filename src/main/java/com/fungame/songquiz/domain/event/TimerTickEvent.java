package com.fungame.songquiz.domain.event;

public record TimerTickEvent(
        String roomId,
        int remainingSeconds
) {
}
