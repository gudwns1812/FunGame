package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.CSQuizDifficulty;

public record CsQuizGameCreateInfo(int totalRound, CSQuizDifficulty difficulty) implements GameCreateInfo {
}
