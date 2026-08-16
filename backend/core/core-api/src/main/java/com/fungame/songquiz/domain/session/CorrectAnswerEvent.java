package com.fungame.songquiz.domain.session;

public record CorrectAnswerEvent(
        Long roomId,
        String nickname,
        String answer
) {
}
