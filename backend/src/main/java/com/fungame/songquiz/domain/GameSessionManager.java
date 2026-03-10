package com.fungame.songquiz.domain;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import com.fungame.songquiz.domain.dto.GameInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameSessionManager {
    private final Map<Long, GameSession> manager = new ConcurrentHashMap<>();

    public GameInfo startGame(Long roomId, Game game, List<String> players) {
        manager.put(roomId, new GameSession(game, players));
        return game.getGameInfo();
    }

    public GameSession getGameSession(Long roomId) {
        return manager.get(roomId);
    }

    public void endGameSession(Long roomId) {
        manager.remove(roomId);
    }

    public Long getGameRoomIdByPlayer(String playerName) {
        return manager.entrySet().stream()
                .filter(entry -> entry.getValue().hasPlayer(playerName))
                .map(Entry::getKey)
                .findAny()
                .orElse(null);
    }
}
