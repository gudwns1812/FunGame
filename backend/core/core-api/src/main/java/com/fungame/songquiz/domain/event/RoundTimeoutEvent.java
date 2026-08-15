package com.fungame.songquiz.domain.event;

public record RoundTimeoutEvent(
        Long roomId,
        String answer
) {
}
