package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.domain.quiz.Quiz;
import com.fungame.songquiz.domain.session.GameSession;
import com.fungame.songquiz.domain.session.GameTimer;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameRoomManager {

    private static final Duration MAX_IDLE = Duration.ofMinutes(30);
    private static final Duration TOUCH_INTERVAL = Duration.ofMinutes(1);

    private final RoomNumberWriter roomNumberWriter;
    private final RoomStore roomStore;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final GameTimer gameTimer;

    private GameRoom getRoom(Long roomId) {
        return roomStore.find(roomId)
                .orElseThrow(() -> new CoreException(ErrorType.GAME_ROOM_NOT_FOUND));
    }

    public GameRoom findRoom(Long roomId) {
        return getRoom(roomId);
    }

    public Long createGameRoom(RoomSettings settings, GamePlayer host) {
        Long roomId = roomNumberWriter.issueNext();
        roomStore.create(GameRoom.create(roomId, settings, host));

        return roomId;
    }

    public JoinResult joinRoom(Long roomId, GamePlayer player) {
        JoinResult result = roomStore.updateTable(roomId, table -> {
            GameRoom gameRoom = table.room();
            gameRoom.touch();

            return gameRoom.isPlaying()
                    ? rejoinPlayingRoom(roomId, table, player)
                    : gameRoom.join(player);
        });

        applicationEventPublisher.publishEvent(new RoomChangedEvent());
        return result;
    }

    private JoinResult rejoinPlayingRoom(Long roomId, RoomTable table, GamePlayer player) {
        GameRoom gameRoom = table.room();
        if (gameRoom.hasPlayer(player.memberId())) {
            return new JoinResult(gameRoom.getPlayerCount(), false, RoomStateInfo.from(gameRoom));
        }

        GameSession gameSession = table.game()
                .filter(game -> game.canRejoin(player.memberId()))
                .orElseThrow(() -> new CoreException(ErrorType.GAME_ALREADY_PLAYING));

        JoinResult result = gameRoom.rejoin(player);
        gameSession.restorePlayer(player);
        log.info("게임 재입장: room {}, member {}", roomId, player.memberId());

        return result;
    }

    public LeaveResult leaveRoom(Long roomId, Long memberId) {
        LeaveResult result = roomStore.updateTable(roomId, table -> {
            GameRoom gameRoom = table.room();
            boolean wasPlaying = gameRoom.isPlaying();
            boolean wasInRoom = gameRoom.hasPlayer(memberId);

            gameRoom.leave(memberId);
            gameRoom.touch();
            table.game().ifPresent(game -> game.removePlayer(memberId));

            if (gameRoom.isEmpty()) {
                return new LeaveResult(true, wasPlaying, wasInRoom, null);
            }

            return new LeaveResult(false, wasPlaying, wasInRoom, RoomStateInfo.from(gameRoom));
        });

        if (result.destroyed()) {
            clearGameOf(roomId);
        }

        applicationEventPublisher.publishEvent(new RoomChangedEvent());
        return result;
    }

    public boolean hasPlayer(Long roomId, Long memberId) {
        return roomStore.find(roomId)
                .map(gameRoom -> gameRoom.hasPlayer(memberId))
                .orElse(false);
    }

    public KickResult kickPlayer(Long roomId, Long hostId, Long targetId) {
        KickResult result = roomStore.update(roomId, gameRoom -> {
            GamePlayer kicked = gameRoom.kick(hostId, targetId);
            return new KickResult(kicked, RoomStateInfo.from(gameRoom));
        });

        applicationEventPublisher.publishEvent(new RoomChangedEvent());
        return result;
    }

    private void clearGameOf(Long roomId) {
        gameTimer.stop(roomId);
    }

    public GameRoom findStartableRoom(Long roomId, Long memberId) {
        GameRoom gameRoom = getRoom(roomId);
        gameRoom.validateStart(memberId);
        return gameRoom;
    }

    public GameSession startGame(Long roomId, Long memberId, Function<GameRoom, Quiz> quizFor) {
        GameSession started = roomStore.updateTable(roomId, table -> {
            GameRoom gameRoom = table.room();
            gameRoom.start(memberId);

            GameSession gameSession = new GameSession(quizFor.apply(gameRoom), gameRoom.getRoomPlayers());
            table.startGame(gameSession);
            return gameSession;
        });

        applicationEventPublisher.publishEvent(new RoomChangedEvent());
        return started;
    }

    public void endGame(Long roomId) {
        try {
            roomStore.updateTable(roomId, table -> {
                table.room().finishGame();
                table.endGame();
                return null;
            });
        } catch (CoreException e) {
            if (e.getType() == ErrorType.GAME_ROOM_NOT_FOUND) {
                return;
            }
            throw e;
        }

        clearGameOf(roomId);
        applicationEventPublisher.publishEvent(new RoomChangedEvent());
    }

    public RoomStateInfo changeSettings(Long roomId, Long memberId, RoomSettings newSettings) {
        RoomStateInfo changed = roomStore.update(roomId, gameRoom -> {
            if (!gameRoom.isHost(memberId)) {
                throw new CoreException(ErrorType.NOT_VALID_HOST);
            }

            gameRoom.changeSettings(newSettings);
            return RoomStateInfo.from(gameRoom);
        });

        applicationEventPublisher.publishEvent(new RoomChangedEvent());
        return changed;
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanupIdleRooms() {
        roomStore.removeIdle(Instant.now().minus(MAX_IDLE)).forEach(roomId -> {
            log.info("유휴 방 정리: {}", roomId);
            afterRemoved(roomId);
        });
    }

    private void afterRemoved(Long roomId) {
        clearGameOf(roomId);
        applicationEventPublisher.publishEvent(new RoomChangedEvent());
    }

    public List<GameRoom> findAllRooms() {
        return roomStore.findAll();
    }

    public MemberLocation locationOf(Long memberId) {
        return roomStore.roomIdOf(memberId)
                .flatMap(roomStore::find)
                .filter(room -> room.hasPlayer(memberId))
                .map(MemberLocation::in)
                .orElseGet(MemberLocation::lobby);
    }

    public MemberLocations locationsOfEveryPlayer() {
        Map<Long, MemberLocation> locationsByMember = new HashMap<>();

        roomStore.findAll().forEach(room -> {
            MemberLocation location = MemberLocation.in(room);
            room.getRoomPlayers().forEach(player -> locationsByMember.put(player.memberId(), location));
        });

        return new MemberLocations(locationsByMember);
    }

    public RoomStateInfo findRoomState(Long roomId) {
        return RoomStateInfo.from(getRoom(roomId));
    }

    public ReadyResult readyPlayer(Long roomId, Long memberId) {
        return roomStore.update(roomId, gameRoom -> {
            gameRoom.touch();

            boolean ready = gameRoom.readyPlayer(memberId);

            return new ReadyResult(ready, gameRoom.isAllReady(), RoomStateInfo.from(gameRoom));
        });
    }

    public void touch(Long roomId) {
        Instant staleBefore = Instant.now().minus(TOUCH_INTERVAL);

        if (getRoom(roomId).isIdle(staleBefore)) {
            roomStore.update(roomId, gameRoom -> {
                gameRoom.touch();
                return null;
            });
        }
    }

    public GameType getGameType(Long roomId) {
        return getRoom(roomId).getSettings().gameType();
    }

    public void healthCheck(Long roomId) {
        getRoom(roomId);
    }
}
