package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.quiz.QuizInfo;
import com.fungame.songquiz.domain.session.GameStateDto;

import java.util.List;

public record GameStateResponse(
        String gameType,
        String category,
        int totalCount,
        int currentRound,
        int totalRound,
        String content,
        List<String> statusData,
        long remainingMillis
) {

    public static GameStateResponse from(GameStateDto state) {
        QuizInfo quizInfo = state.quizInfo();

        return new GameStateResponse(
                quizInfo.gameType(),
                quizInfo.category(),
                quizInfo.totalCount(),
                state.currentRound(),
                state.totalRound(),
                state.content(),
                state.statusData(),
                state.remainingMillis());
    }
}
