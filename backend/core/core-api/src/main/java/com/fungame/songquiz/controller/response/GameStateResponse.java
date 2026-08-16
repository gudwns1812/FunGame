package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.session.GameStateDto;

import java.util.List;

public record GameStateResponse(
        String gameType,
        String category,
        int totalCount,
        int currentRound,
        int totalRound,
        String content,
        List<String> statusData
) {

    public static GameStateResponse from(GameStateDto state) {
        return new GameStateResponse(
                state.gameType(),
                state.category(),
                state.totalCount(),
                state.currentRound(),
                state.totalRound(),
                state.content(),
                state.statusData());
    }
}
