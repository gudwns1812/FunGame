package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.QuizInfo;

public record GameStartEvent(
        Long roomId,
        QuizInfo quizInfo
) {
}
