package com.fungame.songquiz.domain.room;


public record GamePlayerInfo(
        Long memberId,
        String nickname,
        boolean isReady
) {
    public static GamePlayerInfo from(GamePlayer player) {
        return new GamePlayerInfo(player.memberId(), player.nickname(), player.isReady());
    }
}
