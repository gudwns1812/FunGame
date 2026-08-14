package com.fungame.songquiz.domain.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.Set;

@Getter
@Builder
public class HangmanStatusDto {
    private final String currentDisplay;
    private final Set<Character> wrongLetters;
    private final int remainingTries;
    private final String currentTurnPlayer;
    private final boolean isGameOver;
    private final boolean isWin;
}
