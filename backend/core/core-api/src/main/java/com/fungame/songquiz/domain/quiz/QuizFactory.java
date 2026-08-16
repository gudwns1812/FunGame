package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.GameType;

public interface QuizFactory {
    GameType getSupportedType();

    Quiz create(QuizCreateInfo info);
}
