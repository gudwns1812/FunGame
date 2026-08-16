package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.enums.ActionResult;
import com.fungame.songquiz.enums.GameType;

public interface Quiz {
    QuizContent getStatus();

    QuizInfo getQuizInfo();

    GameType getType();

    ActionResult submitAnswer(Long memberId, String answer);

    boolean startProcessing();

    void resetRoundState();

    void dropPlayer(Long memberId);

    void takeBackPlayer(GamePlayer player);

    QuizAnswer getAnswer();

    void startRound();

    boolean isRoundStarted();

    boolean isLast();

    int getCurrentRound();

    int getTotalRound();

    String getHint();
}
