package com.fungame.songquiz.domain;

public record RoundTimeoutEvent(
        String roomId,
        int nextSongIndex
) {
}
