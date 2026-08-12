package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.PlayersInfo;
import com.fungame.songquiz.domain.dto.RoomInfo;
import com.fungame.songquiz.domain.dto.RoomSettingsInfo;
import com.fungame.songquiz.domain.event.PlayerJoinEvent;
import com.fungame.songquiz.domain.event.PlayerLeaveEvent;
import com.fungame.songquiz.domain.event.PlayerReadyEvent;
import com.fungame.songquiz.domain.event.RoomChangedEvent;
import com.fungame.songquiz.domain.event.RoomSettingsChangedEvent;
import com.fungame.songquiz.storage.GameRoomStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoomService {

    private final GameRoomManager gameRoomManager;
    private final GameRoomStore gameRoomStore;
    private final GameService gameService;
    private final RoomPresence roomPresence;
    private final ApplicationEventPublisher applicationEventPublisher;

    @EventListener(ApplicationReadyEvent.class)
    public void resetInterruptedGames() {
        gameRoomStore.markInterruptedGamesWaiting();
    }

    public Long createRoom(RoomSettings settings, String hostName) {
        Long roomId = gameRoomStore.open(settings, hostName);

        gameRoomManager.createGameRoom(roomId, settings, hostName);
        applicationEventPublisher.publishEvent(new RoomChangedEvent());

        return roomId;
    }

    public int joinRoom(Long roomId, String playerName) {
        JoinResult result = gameRoomManager.joinRoom(roomId, playerName);

        if (result.newlyJoined()) {
            applicationEventPublisher.publishEvent(new PlayerJoinEvent(roomId, playerName));
        }

        return result.playerNumber();
    }

    public void leaveRoom(Long roomId, String playerName) {
        LeaveResult result = gameRoomManager.leaveRoom(roomId, playerName);

        if (result.destroyed()) {
            return;
        }

        if (result.wasPlaying()) {
            gameService.handlePlayerLeave(roomId, playerName);
        }

        applicationEventPublisher.publishEvent(new PlayerLeaveEvent(roomId, playerName));
    }

    public List<RoomInfo> findAllRooms() {
        return gameRoomStore.loadAll().stream()
                .map(stored -> RoomInfo.of(stored, roomPresence.countConnectedIn(stored.roomId())))
                .toList();
    }

    public PlayersInfo findUsers(Long roomId) {
        return gameRoomManager.findRoomUsers(roomId);
    }

    public RoomSettingsInfo findSettings(Long roomId) {
        GameRoom gameRoom = gameRoomManager.findRoom(roomId);
        return RoomSettingsInfo.from(gameRoom);
    }

    public RoomSettingsInfo changeSettings(Long roomId, String nickname, RoomSettings newSettings) {
        GameRoom gameRoom = gameRoomManager.changeSettings(roomId, nickname, newSettings);
        RoomSettingsInfo changed = RoomSettingsInfo.from(gameRoom);

        applicationEventPublisher.publishEvent(new RoomSettingsChangedEvent(roomId, changed));

        return changed;
    }

    public void readyPlayer(Long roomId, String playerName) {
        ReadyResult result = gameRoomManager.readyPlayer(roomId, playerName);

        applicationEventPublisher.publishEvent(new PlayerReadyEvent(roomId, playerName, result.ready(), result.isAllReady()));
    }

    public void healthCheck(Long roomId) {
        gameRoomManager.healthCheck(roomId);
    }
}
