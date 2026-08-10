package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.PlayersInfo;
import com.fungame.songquiz.domain.event.RoomChangedEvent;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import com.fungame.songquiz.support.lock.LockContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameRoomManager {
    private final Map<Long, GameRoom> gameRooms = new ConcurrentHashMap<>();
    private final LockContext lockContext;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final GameTimer gameTimer;
    private final GameSessionManager gameSessionManager;

    private static final long MAX_IDLE_MINUTES = 30;

    private GameRoom getRoom(Long roomId) {
        GameRoom gameRoom = gameRooms.get(roomId);
        if (gameRoom == null) {
            throw new CoreException(ErrorType.GAME_ROOM_NOT_FOUND);
        }

        return gameRoom;
    }

    public GameRoom findRoom(Long roomId) {
        return getRoom(roomId);
    }

    public Map<Long, GameRoom> getRooms() {
        return Map.copyOf(gameRooms);
    }

    public void createGameRoom(Long roomId, String title, Game game, String host, int maxPlayer) {
        GameRoom gameRoom = GameRoom.create(title, game, List.of(host), maxPlayer, host);
        gameRooms.put(roomId, gameRoom);

        lockContext.createLockWithLockKey(roomId);
    }

    public JoinResult joinRoom(Long roomId, String playerName) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.touch();

            if (!gameRoom.isPlaying()) {
                return gameRoom.join(playerName);
            }

            return rejoinPlayingRoom(roomId, gameRoom, playerName);
        });
    }

    private JoinResult rejoinPlayingRoom(Long roomId, GameRoom gameRoom, String playerName) {
        if (gameRoom.hasPlayer(playerName)) {
            return new JoinResult(gameRoom.getPlayerCount(), false);
        }

        GameSession gameSession = gameSessionManager.getGameSession(roomId);
        if (gameSession == null || !gameSession.canRejoin(playerName)) {
            throw new CoreException(ErrorType.GAME_ALREADY_PLAYING);
        }

        JoinResult result = gameRoom.rejoin(playerName);
        gameSession.restorePlayer(playerName);
        log.info("게임 재입장: room {}, player {}", roomId, playerName);

        return result;
    }

    public LeaveResult leaveRoom(Long roomId, String playerName) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            boolean wasPlaying = gameRoom.isPlaying();

            gameRoom.leave(playerName);
            gameRoom.touch();

            if (gameRoom.isEmpty()) {
                deleteRoom(roomId);
                return new LeaveResult(true, wasPlaying);
            }

            return new LeaveResult(false, wasPlaying);
        });
    }

    private void deleteRoom(Long roomId) {
        if (gameRooms.remove(roomId) == null) {
            return;
        }

        gameTimer.stop(roomId);
        gameSessionManager.endGameSession(roomId);
        lockContext.deleteLock(roomId);
        applicationEventPublisher.publishEvent(new RoomChangedEvent());
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
            deleteRoom(roomId);
        });
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanupIdleRooms() {
        Instant threshold = Instant.now().minus(MAX_IDLE_MINUTES, ChronoUnit.MINUTES);

        List<Long> idleRoomIds = gameRooms.entrySet().stream()
                .filter(entry -> entry.getValue().isIdle(threshold))
                .map(Map.Entry::getKey)
                .toList();

        idleRoomIds.forEach(roomId -> lockContext.processWithLockKey(roomId, () -> {
            log.info("유휴 방 정리: {}", roomId);
            deleteRoom(roomId);
        }));
    }

    public PlayersInfo findRoomUsers(Long roomId) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);

            return PlayersInfo.from(gameRoom);
        });
    }

    public ReadyResult readyPlayer(Long roomId, String playerName) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.touch();

            boolean ready = gameRoom.readyPlayer(playerName);
            return new ReadyResult(ready, gameRoom.isAllReady());
        });
    }

    public void touch(Long roomId) {
        getRoom(roomId).touch();
    }

    public GameType getGameType(Long roomId) {
        return getRoom(roomId).getGame().getType();
    }

    public void healthCheck(Long roomId) {
        getRoom(roomId);
    }
}
