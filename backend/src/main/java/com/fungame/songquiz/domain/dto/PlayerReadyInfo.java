package com.fungame.songquiz.domain.dto;

import com.fungame.songquiz.domain.ReadyResult;

public record PlayerReadyInfo(
        Long memberId,
        boolean ready,
        boolean isAllReady
) {
    public static PlayerReadyInfo of(Long memberId, ReadyResult result) {
        return new PlayerReadyInfo(memberId, result.ready(), result.isAllReady());
    }
}
