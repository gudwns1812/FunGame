package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameAnswerDto;
import com.fungame.songquiz.domain.dto.GameContentDto;
import com.fungame.songquiz.domain.dto.GameInfo;

public interface Game {
    GameContentDto getContent();

    GameInfo getGameInfo();

    boolean isCorrect(String answer);

    GameAnswerDto getAnswer();

    void nextRound();

    boolean isLast();

    int getCurrentRound();

    int getTotalRound();
}
