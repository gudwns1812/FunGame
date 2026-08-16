package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.session.GameAction;
import com.fungame.songquiz.enums.ActionResult;
import com.fungame.songquiz.enums.GameType;

import java.util.List;

public interface Game {
    GameContentDto getStatus();

    GameInfo getGameInfo();

    GameType getType();

    ActionResult handleAction(GameAction action);

    boolean startProcessing();

    void resetRoundState();

    void setPlayers(List<GamePlayer> players);

    void removePlayer(Long memberId);

    void restorePlayer(GamePlayer player);

    GameAnswerDto getAnswer();

    void startRound();

    boolean isLast();

    int getCurrentRound();

    int getTotalRound();

    String getHint();
}
