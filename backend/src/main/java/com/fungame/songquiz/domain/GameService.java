package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameStateDto;

import java.util.List;
public interface GameService {
    List<GameType> getSupportTypes();

    void startGame(Long roomId, String nickname);

    void processAnswer(Long roomId, String playerName, String message);

    void handleAction(Long roomId, GameAction action);

    void increaseSkipVote(Long roomId, String playerName);

    List<PlayerScore> getPlayerRanks(Long roomId);

    void startRound(Long roomId);

    /**
     * 게임 진행 중 플레이어가 이탈했을 때 게임별 후속 처리를 한다.
     * 진행 상태에서 이탈자를 제거하고, 게임을 이어갈 수 없으면 종료시킨다.
     */
    void handlePlayerLeave(Long roomId, String playerName);

    /**
     * 재입장한 클라이언트가 화면을 복원할 수 있도록 현재 진행 상태를 돌려준다.
     */
    GameStateDto getPlayState(Long roomId);
}

