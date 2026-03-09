package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameAnswerDto;
import com.fungame.songquiz.domain.dto.GameContentDto;
import com.fungame.songquiz.domain.dto.GameInfo;
import java.util.List;

public interface Game {
    GameContentDto getStatus();

    GameInfo getGameInfo();

    GameType getType();

    ActionResult handleAction(GameAction action);

    boolean startProcessing();

    void resetRoundState();

    void setPlayers(List<String> players);

    GameAnswerDto getAnswer();

    void nextRound();

    boolean isLast();

    int getCurrentRound();

    int getTotalRound();
}
