package com.fungame.songquiz.domain.room;

public record PlayerReadyEvent(
        Long roomId,
        Long memberId,
        String nickname,
        boolean ready,
        boolean isAllReady
) {
}
