package com.fungame.songquiz.domain;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class GameServiceRouter implements GameService {

    private final List<GameService> services;
    private final GameRoomManager roomManager;
    private final Map<GameType, GameService> serviceCache = new ConcurrentHashMap<>();

    @Autowired
    public GameServiceRouter(List<GameService> services, GameRoomManager roomManager) {
        // 자신을 제외한 실제 게임 엔진들만 필터링
        this.services = services.stream()
                .filter(s -> !(s instanceof GameServiceRouter))
                .toList();
        this.roomManager = roomManager;
    }

    private GameService getService(Long roomId) {
        GameType type = roomManager.getGameType(roomId);
        return serviceCache.computeIfAbsent(type, t -> 
            services.stream()
                .filter(s -> s.supports(t))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No GameService found for type: " + t))
        );
    }

    @Override
    public void startGame(Long roomId, String nickname) {
        getService(roomId).startGame(roomId, nickname);
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
    public boolean supports(GameType type) {
        return services.stream().anyMatch(s -> s.supports(type));
    }
}
