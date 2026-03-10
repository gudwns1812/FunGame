package com.fungame.songquiz.domain.event;

public record PlayerReadyEvent(
        Long roomId,
        String player,
        boolean ready,
        boolean isAllReady
) {
}
