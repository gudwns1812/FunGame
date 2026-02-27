package com.fungame.songquiz.domain;

public record TimerTickEvent(
        String roomId,
        int remainingSeconds
) {
}
