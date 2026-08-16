package com.fungame.songquiz.domain.session;

public record RoundTimeoutEvent(
        Long roomId,
        String answer
) {
}
