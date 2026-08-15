package com.fungame.songquiz.domain.gamecreator;

import com.fungame.songquiz.enums.CSQuizDifficulty;

public record CsQuizGameCreateInfo(int totalRound, CSQuizDifficulty difficulty) implements GameCreateInfo {
}
