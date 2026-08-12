package com.fungame.songquiz.domain;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class GameRoom {
    private final GamePlayers players;
    private RoomSettings settings;
    private Game game;
    private GameRoomStatus status;
    private Instant lastActivityTime;

    private GameRoom(RoomSettings settings, GamePlayers players, GameRoomStatus status, Instant lastActivityTime) {
        this.settings = settings;
        this.players = players;
        this.status = status;
        this.lastActivityTime = lastActivityTime;
    }

    public static GameRoom create(RoomSettings settings, List<String> initialPlayers, String host) {
        return new GameRoom(
                settings,
                new GamePlayers(initialPlayers, settings.maxPlayers(), host),
                GameRoomStatus.WAITING,
                Instant.now());
    }

    public static GameRoom restore(RoomSettings settings, List<GamePlayer> players, String host, Instant lastActivityTime) {
        return new GameRoom(
                settings,
                GamePlayers.restore(players, settings.maxPlayers(), host),
                GameRoomStatus.WAITING,
                lastActivityTime);
    }

    public String getTitle() {
        return settings.title();
    }

    public JoinResult join(String playerName) {
        validateJoin();
        boolean newlyJoined = players.add(playerName);
        return new JoinResult(players.getCurrentCount(), newlyJoined);
    }

    private void validateJoin() {
        if (status == GameRoomStatus.PLAYING) {
            throw new CoreException(ErrorType.GAME_ALREADY_PLAYING);
        }
    }

    public JoinResult rejoin(String playerName) {
        boolean newlyJoined = players.add(playerName);
        return new JoinResult(players.getCurrentCount(), newlyJoined);
    }

    public void leave(String player) {
        players.remove(player);
    }

    public List<String> getRoomPlayers() {
        return players.getPlayers();
    }

    public void start(String nickname, Game startingGame) {
        validateStart(nickname);
        this.game = startingGame;
        this.status = GameRoomStatus.PLAYING;
    }

    private void validateStart(String nickname) {
        if (!hasHostName(nickname)) {
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
        this.game = null;
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
        if (newSettings.gameType() != settings.gameType()) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
    }

    public boolean isEmpty() {
        return players.getCurrentCount() == 0;
    }

    public boolean isPlaying() {
        return status == GameRoomStatus.PLAYING;
    }

    public boolean hasPlayer(String playerName) {
        return players.getPlayers().contains(playerName);
    }

    public int getPlayerCount() {
        return players.getCurrentCount();
    }

    public boolean hasHostName(String name) {
        return players.getHost().equals(name);
    }

    public boolean readyPlayer(String player) {
        return players.readyPlayer(player);
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
