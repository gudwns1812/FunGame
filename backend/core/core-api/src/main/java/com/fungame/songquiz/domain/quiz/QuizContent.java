package com.fungame.songquiz.domain.quiz;

import java.util.List;

public record QuizContent(
        String description,
        List<String> data
) {

    public static QuizContent of(String description) {
        return new QuizContent(description, List.of(description));
    }
}
