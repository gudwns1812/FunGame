package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.GameAnswerDto;

public record RoundEndEvent(
        Long roomId,
        Long winnerMemberId,
        String winnerNickname,
        GameAnswerDto answer
) {
}
