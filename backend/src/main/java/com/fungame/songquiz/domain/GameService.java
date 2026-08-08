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

    void handlePlayerLeave(Long roomId, String playerName);

    GameStateDto getPlayState(Long roomId);
}

