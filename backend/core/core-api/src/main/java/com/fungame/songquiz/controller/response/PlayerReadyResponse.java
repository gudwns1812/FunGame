package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.room.PlayerReadyInfo;

public record PlayerReadyResponse(
        Long memberId,
        boolean ready,
        boolean isAllReady
) {

    public static PlayerReadyResponse from(PlayerReadyInfo info) {
        return new PlayerReadyResponse(info.memberId(), info.ready(), info.isAllReady());
    }
}
