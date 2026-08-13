package com.fungame.songquiz.domain.dto;

import com.fungame.songquiz.domain.GamePlayer;

public record GamePlayerInfo(
        Long memberId,
        String nickname,
        boolean isReady
) {
    public static GamePlayerInfo from(GamePlayer player) {
        return new GamePlayerInfo(player.memberId(), player.nickname(), player.isReady());
    }
}
