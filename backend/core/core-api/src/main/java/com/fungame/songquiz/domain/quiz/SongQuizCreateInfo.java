package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.Category;

public record SongQuizCreateInfo(Category category, int songCount) implements QuizCreateInfo {
}
