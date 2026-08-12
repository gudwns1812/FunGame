package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.PlayersInfo;
import com.fungame.songquiz.domain.event.RoomChangedEvent;
import com.fungame.songquiz.storage.GameRoomStore;
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
    private final GameRoomStore gameRoomStore;
    private final GameFactories gameFactories;

    private static final long MAX_IDLE_MINUTES = 30;

    private GameRoom getRoom(Long roomId) {
        return gameRooms.computeIfAbsent(roomId, this::restoreFromStore);
    }

    private GameRoom restoreFromStore(Long roomId) {
        StoredRoom stored = gameRoomStore.load(roomId)
                .orElseThrow(() -> new CoreException(ErrorType.GAME_ROOM_NOT_FOUND));

        log.info("메모리에 없는 방을 저장소에서 복원한다: {}", roomId);

        return GameRoom.restore(
                stored.settings(),
                stored.players(),
                stored.host(),
                stored.lastActivityTime());
    }

    public GameRoom findRoom(Long roomId) {
        return getRoom(roomId);
    }

    public void createGameRoom(Long roomId, RoomSettings settings, String host) {
        GameRoom gameRoom = GameRoom.create(settings, List.of(host), host);
        gameRooms.put(roomId, gameRoom);

        lockContext.createLockWithLockKey(roomId);
    }

    public JoinResult joinRoom(Long roomId, String playerName) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.touch();

            JoinResult result = gameRoom.isPlaying()
                    ? rejoinPlayingRoom(roomId, gameRoom, playerName)
                    : gameRoom.join(playerName);

            gameRoomStore.save(roomId, gameRoom);
            return result;
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

            gameRoomStore.save(roomId, gameRoom);
            return new LeaveResult(false, wasPlaying);
        });
    }

    private void deleteRoom(Long roomId) {
        if (gameRooms.remove(roomId) == null && gameRoomStore.load(roomId).isEmpty()) {
            return;
        }

        gameTimer.stop(roomId);
        gameSessionManager.endGameSession(roomId);
        gameRoomStore.delete(roomId);
        lockContext.deleteLock(roomId);
        applicationEventPublisher.publishEvent(new RoomChangedEvent());
    }

    public GameRoom startGame(Long roomId, String nickname) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.start(nickname, gameFactories.create(gameRoom.getSettings()));
            gameRoomStore.save(roomId, gameRoom);
            return gameRoom;
        });
    }

    public void endGame(Long roomId) {
        lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = gameRooms.get(roomId);
            if (gameRoom == null) {
                return;
            }

            gameTimer.stop(roomId);
            gameSessionManager.endGameSession(roomId);
            gameRoom.finishGame();

            gameRoomStore.save(roomId, gameRoom);
            applicationEventPublisher.publishEvent(new RoomChangedEvent());
        });
    }

    public GameRoom changeSettings(Long roomId, String nickname, RoomSettings newSettings) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            if (!gameRoom.hasHostName(nickname)) {
                throw new CoreException(ErrorType.NOT_VALID_HOST);
            }

            gameRoom.changeSettings(newSettings);
            gameRoomStore.save(roomId, gameRoom);
            applicationEventPublisher.publishEvent(new RoomChangedEvent());

            return gameRoom;
        });
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanupIdleRooms() {
        Instant threshold = Instant.now().minus(MAX_IDLE_MINUTES, ChronoUnit.MINUTES);

        List<Long> idleRoomIds = gameRoomStore.loadAll().stream()
                .filter(stored -> isIdle(stored, threshold))
                .map(StoredRoom::roomId)
                .toList();

        idleRoomIds.forEach(roomId -> lockContext.processWithLockKey(roomId, () -> {
            log.info("유휴 방 정리: {}", roomId);
            deleteRoom(roomId);
        }));
    }

    private boolean isIdle(StoredRoom stored, Instant threshold) {
        GameRoom liveRoom = gameRooms.get(stored.roomId());

        return liveRoom != null
                ? liveRoom.isIdle(threshold)
                : stored.lastActivityTime().isBefore(threshold);
    }

    public PlayersInfo findRoomUsers(Long roomId) {
        return lockContext.processWithLockKey(roomId, () -> PlayersInfo.from(getRoom(roomId)));
    }

    public ReadyResult readyPlayer(Long roomId, String playerName) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.touch();

            boolean ready = gameRoom.readyPlayer(playerName);
            gameRoomStore.save(roomId, gameRoom);

            return new ReadyResult(ready, gameRoom.isAllReady());
        });
    }

    public void touch(Long roomId) {
        getRoom(roomId).touch();
    }

    public GameType getGameType(Long roomId) {
        return getRoom(roomId).getSettings().gameType();
    }

    public void healthCheck(Long roomId) {
        getRoom(roomId);
    }
}
