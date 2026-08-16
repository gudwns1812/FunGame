package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.QuizContent;

public record RoundStartEvent(
        Long roomId,
        QuizContent content,
        int currentRound,
        int totalRound
) {
}
