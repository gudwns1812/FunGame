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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameRoomManager {
    private final Map<Long, GameRoom> gameRooms = new ConcurrentHashMap<>();
    private final AtomicLong lastIssuedRoomId = new AtomicLong();
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

    public Long createGameRoom(RoomSettings settings, GamePlayer host) {
        Long roomId = lastIssuedRoomId.incrementAndGet();
        gameRooms.put(roomId, GameRoom.create(roomId, settings, host));

        return roomId;
    }

    public JoinResult joinRoom(Long roomId, GamePlayer player) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.touch();

            JoinResult result = gameRoom.isPlaying()
                    ? rejoinPlayingRoom(roomId, gameRoom, player)
                    : gameRoom.join(player);

            applicationEventPublisher.publishEvent(new RoomChangedEvent());
            return result;
        });
    }

    private JoinResult rejoinPlayingRoom(Long roomId, GameRoom gameRoom, GamePlayer player) {
        if (gameRoom.hasPlayer(player.memberId())) {
            return new JoinResult(gameRoom.getPlayerCount(), false, RoomStateInfo.from(gameRoom));
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
                return new LeaveResult(true, wasPlaying, nickname, null);
            }

            applicationEventPublisher.publishEvent(new RoomChangedEvent());
            return new LeaveResult(false, wasPlaying, nickname, RoomStateInfo.from(gameRoom));
        });
    }

    public boolean hasPlayer(Long roomId, Long memberId) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom liveRoom = gameRooms.get(roomId);
            return liveRoom != null && liveRoom.hasPlayer(memberId);
        });
    }

    public KickResult kickPlayer(Long roomId, Long hostId, Long targetId) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);

            GamePlayer kicked = gameRoom.kick(hostId, targetId);
            applicationEventPublisher.publishEvent(new RoomChangedEvent());

            return new KickResult(kicked, RoomStateInfo.from(gameRoom));
        });
    }

    private void deleteRoom(Long roomId) {
        if (gameRooms.remove(roomId) == null) {
            return;
        }

        gameTimer.stop(roomId);
        gameSessionManager.endGameSession(roomId);
        applicationEventPublisher.publishEvent(new RoomChangedEvent());
    }

    public GameRoom findStartableRoom(Long roomId, Long memberId) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.validateStart(memberId);
            return gameRoom;
        });
    }

    public GameRoom startGame(Long roomId, Long memberId) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.start(memberId);
            applicationEventPublisher.publishEvent(new RoomChangedEvent());
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

            applicationEventPublisher.publishEvent(new RoomChangedEvent());
        });
    }

    public RoomStateInfo changeSettings(Long roomId, Long memberId, RoomSettings newSettings) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            if (!gameRoom.isHost(memberId)) {
                throw new CoreException(ErrorType.NOT_VALID_HOST);
            }

            gameRoom.changeSettings(newSettings);
            applicationEventPublisher.publishEvent(new RoomChangedEvent());

            return RoomStateInfo.from(gameRoom);
        });
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanupIdleRooms() {
        Instant threshold = Instant.now().minus(MAX_IDLE_MINUTES, ChronoUnit.MINUTES);

        List<Long> idleRoomIds = gameRooms.values().stream()
                .filter(room -> room.isIdle(threshold))
                .map(GameRoom::getRoomId)
                .toList();

        idleRoomIds.forEach(roomId -> lockContext.processWithLockKey(roomId, () -> {
            log.info("유휴 방 정리: {}", roomId);
            deleteRoom(roomId);
        }));
    }

    public List<GameRoom> findAllRooms() {
        return List.copyOf(gameRooms.values());
    }

    public MemberLocation locationOf(Long memberId) {
        return gameRooms.values().stream()
                .filter(room -> room.hasPlayer(memberId))
                .findFirst()
                .map(MemberLocation::in)
                .orElseGet(MemberLocation::lobby);
    }

    public MemberLocations locationsOfEveryPlayer() {
        Map<Long, MemberLocation> locationsByMember = new HashMap<>();

        gameRooms.values().forEach(room -> {
            MemberLocation location = MemberLocation.in(room);
            room.getRoomPlayers().forEach(player -> locationsByMember.put(player.memberId(), location));
        });

        return new MemberLocations(locationsByMember);
    }

    public RoomStateInfo findRoomState(Long roomId) {
        return lockContext.processWithLockKey(roomId, () -> RoomStateInfo.from(getRoom(roomId)));
    }

    public ReadyResult readyPlayer(Long roomId, Long memberId) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.touch();

            boolean ready = gameRoom.readyPlayer(memberId);

            return new ReadyResult(ready, gameRoom.isAllReady(), gameRoom.nicknameOf(memberId),
                    RoomStateInfo.from(gameRoom));
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
