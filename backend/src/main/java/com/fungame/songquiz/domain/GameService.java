package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameStateDto;

import java.util.List;
public interface GameService {
    List<GameType> getSupportTypes();

    void startGame(Long roomId, Long memberId);

    void processAnswer(Long roomId, Long memberId, String message);

    void handleAction(Long roomId, GameAction action);

    void increaseSkipVote(Long roomId, Long memberId);

    List<PlayerScore> getPlayerRanks(Long roomId);

    void startRound(Long roomId);

    void handlePlayerLeave(Long roomId, Long memberId);

    GameStateDto getPlayState(Long roomId);
}

