package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.GameAnswer;

public record RoundEndEvent(
        Long roomId,
        Long winnerMemberId,
        String winnerNickname,
        GameAnswer answer
) {
}
