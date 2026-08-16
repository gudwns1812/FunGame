package com.fungame.songquiz.domain.session;

public record QuizGameHintEvent(
        Long roomId,
        String hint
) {
}
