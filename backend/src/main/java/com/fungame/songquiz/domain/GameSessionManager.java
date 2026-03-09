package com.fungame.songquiz.domain;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import com.fungame.songquiz.domain.dto.GameInfo;
import com.fungame.songquiz.domain.dto.GameSkipInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameSessionManager {
    private final Map<Long, GameSession> manager = new ConcurrentHashMap<>();
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

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

    public GameSkipInfo increaseSkipVote(Long roomId, String player) {
        return processWithLock(roomId, () -> {
            GameSession gameSession = getGameSession(roomId);
            return gameSession.voteSkip(player);
        });
    }

    private <T> T processWithLock(Long roomId, Supplier<T> supplier) {
        ReentrantLock lock = locks.computeIfAbsent(roomId, k -> new ReentrantLock());
        lock.lock();

        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }
}
