package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.CSQuizDifficulty;

public record CsQuizCreateInfo(int totalRound, CSQuizDifficulty difficulty) implements QuizCreateInfo {
}
