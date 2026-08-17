package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.domain.session.GameSession;
import com.fungame.songquiz.domain.session.GameSessionManager;
import com.fungame.songquiz.domain.session.GameTimer;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
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
    private final GameRoomReader gameRoomReader;
    private final GameRoomWriter gameRoomWriter;

    private static final long MAX_IDLE_MINUTES = 30;

    private GameRoom getRoom(Long roomId) {
        return gameRooms.computeIfAbsent(roomId, this::restoreFromStore);
    }

    private GameRoom restoreFromStore(Long roomId) {
        GameRoom stored = gameRoomReader.load(roomId)
                .orElseThrow(() -> new CoreException(ErrorType.GAME_ROOM_NOT_FOUND));

        log.info("메모리에 없는 방을 저장소에서 복원한다: {}", roomId);

        return stored;
    }

    public GameRoom findRoom(Long roomId) {
        return getRoom(roomId);
    }

    public void createGameRoom(Long roomId, RoomSettings settings, GamePlayer host) {
        GameRoom gameRoom = GameRoom.create(roomId, settings, host);
        gameRooms.put(roomId, gameRoom);

        lockContext.createLockWithLockKey(roomId);
    }

    public JoinResult joinRoom(Long roomId, GamePlayer player) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.touch();

            JoinResult result = gameRoom.isPlaying()
                    ? rejoinPlayingRoom(roomId, gameRoom, player)
                    : gameRoom.join(player);

            gameRoomWriter.save(gameRoom);
            return result;
        });
    }

    private JoinResult rejoinPlayingRoom(Long roomId, GameRoom gameRoom, GamePlayer player) {
        if (gameRoom.hasPlayer(player.memberId())) {
            return new JoinResult(gameRoom.getPlayerCount(), false);
        }

        GameSession gameSession = gameSessionManager.getGameSession(roomId);
        if (gameSession == null || !gameSession.canRejoin(player.memberId())) {
            throw new CoreException(ErrorType.GAME_ALREADY_PLAYING);
        }

        JoinResult result = gameRoom.rejoin(player);
        gameSession.restorePlayer(player);
        log.info("게임 재입장: room {}, member {}", roomId, player.memberId());

        return result;
    }

    public LeaveResult leaveRoom(Long roomId, Long memberId) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            boolean wasPlaying = gameRoom.isPlaying();
            String nickname = gameRoom.nicknameOf(memberId);

            gameRoom.leave(memberId);
            gameRoom.touch();

            if (gameRoom.isEmpty()) {
                deleteRoom(roomId);
                return new LeaveResult(true, wasPlaying, nickname);
            }

            gameRoomWriter.save(gameRoom);
            return new LeaveResult(false, wasPlaying, nickname);
        });
    }

    public boolean hasPlayer(Long roomId, Long memberId) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom liveRoom = gameRooms.get(roomId);
            return liveRoom != null && liveRoom.hasPlayer(memberId);
        });
    }

    public GamePlayer kickPlayer(Long roomId, Long hostId, Long targetId) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);

            GamePlayer kicked = gameRoom.kick(hostId, targetId);
            gameRoomWriter.save(gameRoom);

            return kicked;
        });
    }

    private void deleteRoom(Long roomId) {
        if (gameRooms.remove(roomId) == null && gameRoomReader.load(roomId).isEmpty()) {
            return;
        }

        gameTimer.stop(roomId);
        gameSessionManager.endGameSession(roomId);
        gameRoomWriter.delete(roomId);
        lockContext.deleteLock(roomId);
        applicationEventPublisher.publishEvent(new RoomChangedEvent());
    }

    public GameRoom startGame(Long roomId, Long memberId) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.start(memberId);
            gameRoomWriter.save(gameRoom);
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

            gameRoomWriter.save(gameRoom);
            applicationEventPublisher.publishEvent(new RoomChangedEvent());
        });
    }

    public GameRoom changeSettings(Long roomId, Long memberId, RoomSettings newSettings) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            if (!gameRoom.isHost(memberId)) {
                throw new CoreException(ErrorType.NOT_VALID_HOST);
            }

            gameRoom.changeSettings(newSettings);
            gameRoomWriter.save(gameRoom);
            applicationEventPublisher.publishEvent(new RoomChangedEvent());

            return gameRoom;
        });
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanupIdleRooms() {
        Instant threshold = Instant.now().minus(MAX_IDLE_MINUTES, ChronoUnit.MINUTES);

        List<Long> idleRoomIds = gameRoomReader.loadAll().stream()
                .filter(stored -> isIdle(stored, threshold))
                .map(GameRoom::getRoomId)
                .toList();

        idleRoomIds.forEach(roomId -> lockContext.processWithLockKey(roomId, () -> {
            log.info("유휴 방 정리: {}", roomId);
            deleteRoom(roomId);
        }));
    }

    private boolean isIdle(GameRoom stored, Instant threshold) {
        GameRoom liveRoom = gameRooms.get(stored.getRoomId());

        return liveRoom != null ? liveRoom.isIdle(threshold) : stored.isIdle(threshold);
    }

    public PlayersInfo findRoomUsers(Long roomId) {
        return lockContext.processWithLockKey(roomId, () -> PlayersInfo.from(getRoom(roomId)));
    }

    public ReadyResult readyPlayer(Long roomId, Long memberId) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.touch();

            boolean ready = gameRoom.readyPlayer(memberId);
            gameRoomWriter.save(gameRoom);

            return new ReadyResult(ready, gameRoom.isAllReady(), gameRoom.nicknameOf(memberId));
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
