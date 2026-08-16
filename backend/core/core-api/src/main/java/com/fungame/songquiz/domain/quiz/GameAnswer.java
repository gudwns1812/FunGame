package com.fungame.songquiz.domain.quiz;

public record GameAnswer(
        String answer,
        String explanation
) {

    public static GameAnswer withoutExplanation(String answer) {
        return new GameAnswer(answer, "");
    }
}
