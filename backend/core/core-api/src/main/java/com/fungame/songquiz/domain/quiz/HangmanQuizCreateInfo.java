package com.fungame.songquiz.domain.quiz;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class HangmanQuizCreateInfo implements QuizCreateInfo {
    private final int difficulty;
}
