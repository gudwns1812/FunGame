package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.GameContentDto;

public record RoundStartEvent(
        Long roomId,
        GameContentDto content,
        int currentRound,
        int totalRound
) {
}
