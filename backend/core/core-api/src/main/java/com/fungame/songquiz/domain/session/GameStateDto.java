package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.QuizInfo;

import java.util.List;

public record GameStateDto(
        QuizInfo quizInfo,
        int currentRound,
        int totalRound,
        String content,
        List<String> statusData,
        long remainingMillis
) {
}
