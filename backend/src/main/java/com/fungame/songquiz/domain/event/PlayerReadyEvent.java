package com.fungame.songquiz.domain.event;

public record PlayerReadyEvent(
        Long roomId,
        Long memberId,
        String nickname,
        boolean ready,
        boolean isAllReady
) {
}
