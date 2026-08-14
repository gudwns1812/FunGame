package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameStateDto;
import com.fungame.songquiz.enums.GameType;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Primary
@Component
public class GameServiceRouter implements GameService {

    private final Map<GameType, GameService> services;
    private final GameRoomManager roomManager;

    public GameServiceRouter(List<GameService> services, GameRoomManager roomManager) {
        this.services = new HashMap<>();
        services.stream()
                .filter(s -> !(s instanceof GameServiceRouter))
                .forEach(service ->
                        service.getSupportTypes().forEach(gameType -> this.services.put(gameType, service))
                );
        this.roomManager = roomManager;
    }

    @Override
    public void startGame(Long roomId, Long memberId) {
        getService(roomId).startGame(roomId, memberId);
    }

    private GameService getService(Long roomId) {
        GameType gameType = roomManager.getGameType(roomId);
        return services.get(gameType);
    }

    @Override
    public void processAnswer(Long roomId, Long memberId, String message) {
        getService(roomId).processAnswer(roomId, memberId, message);
    }

    @Override
    public void handleAction(Long roomId, GameAction action) {
        getService(roomId).handleAction(roomId, action);
    }

    @Override
    public void increaseSkipVote(Long roomId, Long memberId) {
        getService(roomId).increaseSkipVote(roomId, memberId);
    }

    @Override
    public List<PlayerScore> getPlayerRanks(Long roomId) {
        return getService(roomId).getPlayerRanks(roomId);
    }

    @Override
    public void startRound(Long roomId) {
        getService(roomId).startRound(roomId);
    }

    @Override
    public void handlePlayerLeave(Long roomId, Long memberId) {
        getService(roomId).handlePlayerLeave(roomId, memberId);
    }

    @Override
    public GameStateDto getPlayState(Long roomId) {
        return getService(roomId).getPlayState(roomId);
    }

    @Override
    public List<GameType> getSupportTypes() {
        return List.of(GameType.NONE);
    }
}
