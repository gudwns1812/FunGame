package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.PlayersInfo;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fungame.songquiz.support.lock.LockContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameRoomManager {
    private final Map<Long, GameRoom> gameRooms = new ConcurrentHashMap<>();
    private final LockContext lockContext;

    private static final long MAX_IDLE_MINUTES = 30;

    private GameRoom getRoom(Long roomId) {
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

        lockContext.createLockWithLockKey(roomId);
    }

    public int joinRoom(Long roomId, String playerName) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.touch();
            return gameRoom.join(playerName);
        });
    }

    public void leaveRoom(Long roomId, String playerName) {
        lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.leave(playerName);
            gameRoom.touch();

            if (gameRoom.isEmpty()) {
                deleteRoom(roomId);
            }
        });
    }

    private void deleteRoom(Long roomId) {
        gameRooms.remove(roomId);
        lockContext.deleteLock(roomId);
    }

    public GameRoom startGame(Long roomId, String nickname) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.start(nickname);
            return gameRoom;
        });
    }

    public void endGame(Long roomId) {
        lockContext.processWithLockKey(roomId, () -> {
            getRoom(roomId).end();
            deleteRoom(roomId);
        });
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanupIdleRooms() {
        Instant threshold = Instant.now().minus(MAX_IDLE_MINUTES, ChronoUnit.MINUTES);

        gameRooms.entrySet().stream()
                .filter(entry -> entry.getValue().isIdle(threshold))
                .forEach(entry -> deleteRoom(entry.getKey()));
    }

    public PlayersInfo findRoomUsers(Long roomId) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = gameRooms.get(roomId);

            return PlayersInfo.from(gameRoom);
        });
    }

    public boolean readyPlayer(Long roomId, String playerName) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = gameRooms.get(roomId);
            gameRoom.touch();

            gameRoom.readyPlayer(playerName);
            return gameRoom.isAllReady();
        });
    }

    public void touch(Long roomId) {
        GameRoom gameRoom = gameRooms.get(roomId);

        gameRoom.touch();
    }
}
