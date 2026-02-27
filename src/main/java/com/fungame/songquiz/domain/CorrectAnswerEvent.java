package com.fungame.songquiz.domain;

public record CorrectAnswerEvent(
        String roomId,
        String nickname,
        String answer,
        int score
) {
}
