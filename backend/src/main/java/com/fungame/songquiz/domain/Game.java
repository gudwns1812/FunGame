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

    /**
     * 게임 도중 이탈한 플레이어를 게임 진행 상태에서 제거한다.
     * 턴제 게임은 이 시점에 턴 순서도 함께 보정해야 한다.
     */
    void removePlayer(String playerName);

    GameAnswerDto getAnswer();

    void startRound();

    boolean isLast();

    int getCurrentRound();

    int getTotalRound();

    String getHint();
}
