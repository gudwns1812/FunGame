package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.GameContent;

public record RoundStartEvent(
        Long roomId,
        GameContent content,
        int currentRound,
        int totalRound
) {
}
