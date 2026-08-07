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

    public int joinRoom(Long roomId, String playerName) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.touch();
            return gameRoom.join(playerName);
        });
    }

    public boolean leaveRoom(Long roomId, String playerName) {
        return lockContext.processWithLockKey(roomId, () -> {
            GameRoom gameRoom = getRoom(roomId);
            gameRoom.leave(playerName);
            gameRoom.touch();

            if (gameRoom.isEmpty()) {
                deleteRoom(roomId);
                return true;
            }

            return false;
        });
    }

    /**
     * 방 삭제의 유일한 통로. 방과 함께 진행 중이던 게임 상태(타이머, 세션)도 반드시 같이 정리한다.
     * 이 정리가 빠지면 아무도 없는 방의 라운드 타이머가 계속 돌거나 GameSession 이 영구히 남는다.
     */
    private void deleteRoom(Long roomId) {
        if (gameRooms.remove(roomId) == null) {
            // 이미 정리된 방이면 중복 이벤트를 발행하지 않는다.
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

        // 삭제 대상을 먼저 확정한 뒤, 다른 참가 흐름과 경합하지 않도록 방별 락 안에서 정리한다.
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

    public record ReadyResult(boolean ready, boolean isAllReady) {
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
