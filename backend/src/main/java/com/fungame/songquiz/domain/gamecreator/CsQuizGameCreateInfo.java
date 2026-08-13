package com.fungame.songquiz.domain.gamecreator;

import com.fungame.songquiz.domain.CSQuizDifficulty;

public record CsQuizGameCreateInfo(int totalRound, CSQuizDifficulty difficulty) implements GameCreateInfo {
}
