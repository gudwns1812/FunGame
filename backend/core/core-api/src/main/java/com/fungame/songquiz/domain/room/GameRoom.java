package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.enums.GameRoomStatus;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class GameRoom {
    private final Long roomId;
    private final GamePlayers players;
    private RoomSettings settings;
    private GameRoomStatus status;
    private Instant lastActivityTime;

    private GameRoom(Long roomId, RoomSettings settings, GamePlayers players, GameRoomStatus status,
                     Instant lastActivityTime) {
        this.roomId = roomId;
        this.settings = settings;
        this.players = players;
        this.status = status;
        this.lastActivityTime = lastActivityTime;
    }

    public static GameRoom create(Long roomId, RoomSettings settings, GamePlayer host) {
        return new GameRoom(
                roomId,
                settings,
                new GamePlayers(List.of(host), settings.maxPlayers(), host.memberId()),
                GameRoomStatus.WAITING,
                Instant.now());
    }

    public static GameRoom restore(Long roomId, RoomSettings settings, List<GamePlayer> players, Long hostId,
                                   GameRoomStatus status, Instant lastActivityTime) {
        return new GameRoom(
                roomId,
                settings,
                new GamePlayers(players, settings.maxPlayers(), hostId),
                status,
                lastActivityTime);
    }

    public String getTitle() {
        return settings.title();
    }

    public GamePlayer getHost() {
        return players.hostPlayer();
    }

    public Long getHostId() {
        return players.getHost();
    }

    public JoinResult join(GamePlayer player) {
        validateJoin();
        boolean newlyJoined = players.add(player);
        return new JoinResult(players.getCurrentCount(), newlyJoined);
    }

    private void validateJoin() {
        if (status == GameRoomStatus.PLAYING) {
            throw new CoreException(ErrorType.GAME_ALREADY_PLAYING);
        }
    }

    public JoinResult rejoin(GamePlayer player) {
        boolean newlyJoined = players.add(player);
        return new JoinResult(players.getCurrentCount(), newlyJoined);
    }

    public void leave(Long memberId) {
        players.remove(memberId);
    }

    public GamePlayer kick(Long hostId, Long targetId) {
        validateKick(hostId, targetId);

        GamePlayer target = players.playerOf(targetId);
        players.remove(targetId);
        touch();

        return target;
    }

    private void validateKick(Long hostId, Long targetId) {
        if (!isHost(hostId)) {
            throw new CoreException(ErrorType.NOT_VALID_HOST);
        }
        if (status == GameRoomStatus.PLAYING) {
            throw new CoreException(ErrorType.GAME_ALREADY_PLAYING);
        }
        if (hostId.equals(targetId)) {
            throw new CoreException(ErrorType.KICK_SELF);
        }
        if (!hasPlayer(targetId)) {
            throw new CoreException(ErrorType.PLAYER_NOT_FOUND);
        }
    }

    public List<GamePlayer> getRoomPlayers() {
        return players.snapshot();
    }

    public void start(Long memberId) {
        validateStart(memberId);
        this.status = GameRoomStatus.PLAYING;
    }

    public void validateStart(Long memberId) {
        if (!isHost(memberId)) {
            throw new CoreException(ErrorType.NOT_VALID_HOST);
        }
        if (isEmpty()) {
            throw new CoreException(ErrorType.GAME_ROOM_PLAYER_EMPTY);
        }
        if (status == GameRoomStatus.PLAYING) {
            throw new CoreException(ErrorType.GAME_ALREADY_PLAYING);
        }
        if (!isAllReady()) {
            throw new CoreException(ErrorType.GAME_ROOM_NOT_ALL_READY);
        }
    }

    public void finishGame() {
        this.status = GameRoomStatus.WAITING;
        players.resetReady();
        touch();
    }

    public void changeSettings(RoomSettings newSettings) {
        validateSettingsChange(newSettings);

        players.changeMaxPlayer(newSettings.maxPlayers());
        this.settings = newSettings;
        players.resetReady();
        touch();
    }

    private void validateSettingsChange(RoomSettings newSettings) {
        if (status == GameRoomStatus.PLAYING) {
            throw new CoreException(ErrorType.GAME_ALREADY_PLAYING);
        }
    }

    public boolean isEmpty() {
        return players.getCurrentCount() == 0;
    }

    public boolean isPlaying() {
        return status == GameRoomStatus.PLAYING;
    }

    public boolean hasPlayer(Long memberId) {
        return players.hasPlayer(memberId);
    }

    public String nicknameOf(Long memberId) {
        return players.nicknameOf(memberId);
    }

    public int getPlayerCount() {
        return players.getCurrentCount();
    }

    public boolean isHost(Long memberId) {
        return players.getHost().equals(memberId);
    }

    public boolean readyPlayer(Long memberId) {
        return players.readyPlayer(memberId);
    }

    public boolean isAllReady() {
        return players.isAllReady();
    }

    public boolean isIdle(Instant threshold) {
        return lastActivityTime.isBefore(threshold);
    }

    public void touch() {
        lastActivityTime = Instant.now();
    }
}
