package com.fungame.songquiz.domain.event;

public record CorrectAnswerEvent(
        Long roomId,
        String nickname,
        String answer
) {
}
