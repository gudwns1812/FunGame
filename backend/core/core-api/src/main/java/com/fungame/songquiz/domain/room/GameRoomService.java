package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.domain.member.MemberPresenceService;
import com.fungame.songquiz.domain.session.GameService;
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
    private final GameRoomReader gameRoomReader;
    private final GameRoomWriter gameRoomWriter;
    private final GameService gameService;
    private final MemberPresenceService memberPresenceService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @EventListener(ApplicationReadyEvent.class)
    public void resetInterruptedGames() {
        gameRoomWriter.markInterruptedGamesWaiting();
        memberPresenceService.clearEveryLocation();
    }

    public Long createRoom(RoomSettings settings, GamePlayer host) {
        Long roomId = gameRoomWriter.open(settings, host);

        gameRoomManager.createGameRoom(roomId, settings, host);
        memberPresenceService.enterWaitingRoom(host.memberId(), roomId);
        applicationEventPublisher.publishEvent(new RoomChangedEvent());

        return roomId;
    }

    public int joinRoom(Long roomId, GamePlayer player) {
        JoinResult result = gameRoomManager.joinRoom(roomId, player);

        rememberWhereMemberIs(roomId, player.memberId());

        if (result.newlyJoined()) {
            applicationEventPublisher.publishEvent(
                    new PlayerJoinEvent(roomId, player));
        }

        return result.playerNumber();
    }

    public void leaveRoom(Long roomId, Long memberId) {
        LeaveResult result = gameRoomManager.leaveRoom(roomId, memberId);

        memberPresenceService.leaveRoom(memberId);

        if (result.destroyed() || result.nickname() == null) {
            return;
        }

        if (result.wasPlaying()) {
            gameService.handlePlayerLeave(roomId, memberId);
        }

        applicationEventPublisher.publishEvent(new PlayerLeaveEvent(roomId,
                GamePlayer.createNewPlayer(memberId, result.nickname())));
    }

    private void rememberWhereMemberIs(Long roomId, Long memberId) {
        if (gameRoomManager.findRoom(roomId).isPlaying()) {
            memberPresenceService.enterPlayingRoom(memberId, roomId);
            return;
        }

        memberPresenceService.enterWaitingRoom(memberId, roomId);
    }

    public List<RoomInfo> findAllRooms() {
        return gameRoomReader.loadAll().stream()
                .map(RoomInfo::from)
                .toList();
    }

    public RoomInfo findRoomInfo(Long roomId) {
        return RoomInfo.from(gameRoomManager.findRoom(roomId));
    }

    public PlayersInfo findUsers(Long roomId) {
        return gameRoomManager.findRoomUsers(roomId);
    }

    public RoomSettingsInfo findSettings(Long roomId) {
        GameRoom gameRoom = gameRoomManager.findRoom(roomId);
        return RoomSettingsInfo.from(gameRoom);
    }

    public RoomSettingsInfo changeSettings(Long roomId, Long memberId, RoomSettings newSettings) {
        GameRoom gameRoom = gameRoomManager.changeSettings(roomId, memberId, newSettings);
        RoomSettingsInfo changed = RoomSettingsInfo.from(gameRoom);

        applicationEventPublisher.publishEvent(new RoomSettingsChangedEvent(roomId, changed));

        return changed;
    }

    public PlayerReadyInfo readyPlayer(Long roomId, Long memberId) {
        ReadyResult result = gameRoomManager.readyPlayer(roomId, memberId);

        applicationEventPublisher.publishEvent(
                new PlayerReadyEvent(roomId,
                        new GamePlayer(memberId, result.nickname(), result.ready()), result.isAllReady()));

        return PlayerReadyInfo.of(memberId, result);
    }

    public void healthCheck(Long roomId) {
        gameRoomManager.healthCheck(roomId);
    }
}
