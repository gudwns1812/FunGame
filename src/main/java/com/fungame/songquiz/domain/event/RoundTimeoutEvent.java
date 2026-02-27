package com.fungame.songquiz.domain.event;

public record RoundTimeoutEvent(
        String roomId,
        int nextSongIndex
) {
}
