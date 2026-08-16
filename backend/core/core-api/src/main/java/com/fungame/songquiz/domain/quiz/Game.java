package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.enums.ActionResult;
import com.fungame.songquiz.enums.GameType;

public interface Game {
    GameContent getStatus();

    GameInfo getGameInfo();

    GameType getType();

    ActionResult submitAnswer(Long memberId, String answer);

    boolean startProcessing();

    void resetRoundState();

    void dropPlayer(Long memberId);

    void takeBackPlayer(GamePlayer player);

    GameAnswer getAnswer();

    void startRound();

    boolean isLast();

    int getCurrentRound();

    int getTotalRound();

    String getHint();
}
