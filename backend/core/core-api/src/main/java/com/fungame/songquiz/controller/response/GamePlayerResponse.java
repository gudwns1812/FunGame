package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.room.GamePlayer;

import java.util.List;

public record GamePlayerResponse(
        Long memberId,
        String nickname,
        boolean isReady
) {

    public static GamePlayerResponse from(GamePlayer player) {
        return new GamePlayerResponse(player.memberId(), player.nickname(), player.isReady());
    }

    public static List<GamePlayerResponse> listFrom(List<GamePlayer> players) {
        return players.stream()
                .map(GamePlayerResponse::from)
                .toList();
    }
}
