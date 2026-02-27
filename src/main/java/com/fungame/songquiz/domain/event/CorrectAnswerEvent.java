package com.fungame.songquiz.domain.event;

public record CorrectAnswerEvent(
        String roomId,
        String nickname,
        String answer,
        int score
) {
}
