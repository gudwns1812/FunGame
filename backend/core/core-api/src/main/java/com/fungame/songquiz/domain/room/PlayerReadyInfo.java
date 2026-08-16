package com.fungame.songquiz.domain.room;


public record PlayerReadyInfo(
        Long memberId,
        boolean ready,
        boolean isAllReady
) {
    public static PlayerReadyInfo of(Long memberId, ReadyResult result) {
        return new PlayerReadyInfo(memberId, result.ready(), result.isAllReady());
    }
}
