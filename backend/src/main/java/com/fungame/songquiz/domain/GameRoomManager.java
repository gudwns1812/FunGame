package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.PlayersInfo;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GameRoomManager {
    private final Map<Long, GameRoom> gameRooms = new ConcurrentHashMap<>();
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final Map<Long, Instant> lastActivityMap = new ConcurrentHashMap<>();

    private static final long MAX_IDLE_MINUTES = 30;

    public GameRoom getRoom(Long roomId) {
        GameRoom gameRoom = gameRooms.get(roomId);
        if (gameRoom == null) {
            throw new CoreException(ErrorType.GAME_ROOM_NOT_FOUND);
        }

        return gameRoom;
    }

    public Map<Long, GameRoom> getRooms() {
        return Map.copyOf(gameRooms);
    }

    public void createGameRoom(Long roomId, String title, Game game, String host, int maxPlayer) {
        GameRoom gameRoom = GameRoom.create(title, game, List.of(host), maxPlayer, host);
        gameRooms.put(roomId, gameRoom);
        locks.put(roomId, new ReentrantLock());
    }

    public int joinRoom(Long roomId, String playerName) {
        return processWithLock(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            return gameRoom.join(playerName);
        });
    }

    public void leaveRoom(Long roomId, String playerName) {
        processWithLock(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.leave(playerName);

            if (gameRoom.isEmpty()) {
                deleteRoom(roomId);
            }
        });
    }

    private void deleteRoom(Long roomId) {
        gameRooms.remove(roomId);
        locks.remove(roomId);
        lastActivityMap.remove(roomId);
    }

    public GameRoom startGame(Long roomId, String nickname) {
        return processWithLock(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.start(nickname);
            return gameRoom;
        });
    }

    public void endGame(Long roomId) {
        processWithLock(roomId, () -> {
            getRoom(roomId).end();
            deleteRoom(roomId);
        });
    }

    private void processWithLock(Long roomId, Runnable runnable) {
        ReentrantLock lock = locks.computeIfAbsent(roomId, k -> new ReentrantLock());
        lock.lock();

        try {
            updateActivityTime(roomId);
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    private <T> T processWithLock(Long roomId, Supplier<T> supplier) {
        ReentrantLock lock = locks.computeIfAbsent(roomId, k -> new ReentrantLock());
        lock.lock();

        try {
            updateActivityTime(roomId);
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    public void updateActivityTime(Long roomId) {
        lastActivityMap.put(roomId, Instant.now());
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanupIdleRooms() {
        Instant threshold = Instant.now().minus(MAX_IDLE_MINUTES, ChronoUnit.MINUTES);

        lastActivityMap.forEach((roomId, lastActivity) -> {
            if (lastActivity.isBefore(threshold)) {
                log.info("[자동 정리] {}분 동안 활동 없는 방 삭제 시도: {}", MAX_IDLE_MINUTES, roomId);

                processWithLock(roomId, () -> {
                    deleteRoom(roomId);
                    return null;
                });
            }
        });
    }

    public PlayersInfo findRoomUsers(Long roomId) {
        return processWithLock(roomId, () -> {
            GameRoom gameRoom = gameRooms.get(roomId);

            return PlayersInfo.from(gameRoom);
        });
    }
}
