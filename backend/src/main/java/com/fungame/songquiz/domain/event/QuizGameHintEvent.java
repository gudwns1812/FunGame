package com.fungame.songquiz.domain.event;

public record QuizGameHintEvent(
        Long roomId,
        String hint
) {
}
