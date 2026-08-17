package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.domain.session.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoomService {

    private final GameRoomManager gameRoomManager;
    private final GameService gameService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public Long createRoom(RoomSettings settings, GamePlayer host) {
        Long roomId = gameRoomManager.createGameRoom(settings, host);

        applicationEventPublisher.publishEvent(new RoomChangedEvent());

        return roomId;
    }

    public int joinRoom(Long roomId, GamePlayer player) {
        JoinResult result = gameRoomManager.joinRoom(roomId, player);

        if (result.newlyJoined()) {
            applicationEventPublisher.publishEvent(
                    new PlayerJoinEvent(roomId, player, result.state()));
        }

        return result.playerNumber();
    }

    public void leaveRoom(Long roomId, Long memberId) {
        LeaveResult result = gameRoomManager.leaveRoom(roomId, memberId);

        if (result.nickname() == null) {
            return;
        }

        if (result.destroyed()) {
            return;
        }

        if (result.wasPlaying()) {
            gameService.handlePlayerLeave(roomId, memberId);
        }

        applicationEventPublisher.publishEvent(new PlayerLeaveEvent(roomId,
                GamePlayer.createNewPlayer(memberId, result.nickname()), result.state()));
    }

    public void kickPlayer(Long roomId, Long hostId, Long targetId) {
        KickResult result = gameRoomManager.kickPlayer(roomId, hostId, targetId);

        applicationEventPublisher.publishEvent(new PlayerKickedEvent(roomId, result.kicked(), result.state()));
    }

    public boolean hasPlayer(Long roomId, Long memberId) {
        return gameRoomManager.hasPlayer(roomId, memberId);
    }

    public MemberLocation findLocationOf(Long memberId) {
        return gameRoomManager.locationOf(memberId);
    }

    public MemberLocations findEveryLocation() {
        return gameRoomManager.locationsOfEveryPlayer();
    }

    public List<RoomInfo> findAllRooms() {
        return gameRoomManager.findAllRooms().stream()
                .map(RoomInfo::from)
                .toList();
    }

    public RoomInfo findRoomInfo(Long roomId) {
        return RoomInfo.from(gameRoomManager.findRoom(roomId));
    }

    public RoomStateInfo findRoomState(Long roomId) {
        return gameRoomManager.findRoomState(roomId);
    }

    public RoomStateInfo changeSettings(Long roomId, Long memberId, RoomSettings newSettings) {
        RoomStateInfo changed = gameRoomManager.changeSettings(roomId, memberId, newSettings);

        applicationEventPublisher.publishEvent(new RoomSettingsChangedEvent(roomId, changed));

        return changed;
    }

    public PlayerReadyInfo readyPlayer(Long roomId, Long memberId) {
        ReadyResult result = gameRoomManager.readyPlayer(roomId, memberId);

        applicationEventPublisher.publishEvent(
                new PlayerReadyEvent(roomId,
                        new GamePlayer(memberId, result.nickname(), result.ready()), result.isAllReady(),
                        result.state()));

        return PlayerReadyInfo.of(memberId, result);
    }

    public void healthCheck(Long roomId) {
        gameRoomManager.healthCheck(roomId);
    }
}
