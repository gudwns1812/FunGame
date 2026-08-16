package com.fungame.songquiz.domain.quiz;

public record QuizAnswer(
        String answer,
        String explanation
) {

    public static QuizAnswer withoutExplanation(String answer) {
        return new QuizAnswer(answer, "");
    }
}
