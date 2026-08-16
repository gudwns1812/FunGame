package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.QuizAnswer;

public record RoundEndEvent(
        Long roomId,
        Long winnerMemberId,
        String winnerNickname,
        QuizAnswer answer
) {
}
