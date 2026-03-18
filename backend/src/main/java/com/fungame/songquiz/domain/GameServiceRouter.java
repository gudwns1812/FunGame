package com.fungame.songquiz.domain;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public GameServiceRouter(List<GameService> services, GameRoomManager roomManager) {
        // 자신을 제외한 실제 게임 엔진들만 필터링
        this.services = new HashMap<>();
        services.stream()
                .filter(s -> !(s instanceof GameServiceRouter))
                .forEach(service ->
                        service.getSupportTypes().forEach(gameType -> this.services.put(gameType, service))
                );
        this.roomManager = roomManager;
    }

    @Override
    public void startGame(Long roomId, String nickname) {
        getService(roomId).startGame(roomId, nickname);
    }

    private GameService getService(Long roomId) {
        GameType gameType = roomManager.getGameType(roomId);
        return services.get(gameType);
    }

    @Override
    public void processAnswer(Long roomId, String playerName, String message) {
        getService(roomId).processAnswer(roomId, playerName, message);
    }

    @Override
    public void handleAction(Long roomId, GameAction action) {
        getService(roomId).handleAction(roomId, action);
    }

    @Override
    public void increaseSkipVote(Long roomId, String playerName) {
        getService(roomId).increaseSkipVote(roomId, playerName);
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
    public List<GameType> getSupportTypes() {
        return List.of(GameType.NONE);
    }
}
