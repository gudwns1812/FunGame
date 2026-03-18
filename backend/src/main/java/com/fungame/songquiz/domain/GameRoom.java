package com.fungame.songquiz.domain;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class GameRoom {
    private final String title;
    private final Game game;
    private final GamePlayers players;
    private GameRoomStatus status;
    private Instant lastActivityTime;

    private GameRoom(String title, Game game, GamePlayers players) {
        this.title = title;
        this.game = game;
        this.players = players;
        this.status = GameRoomStatus.WAITING;
        this.lastActivityTime = Instant.now();
    }

    public int join(String playerName) {
        validateJoin();
        players.add(playerName);
        return players.getCurrentCount();
    }

    private void validateJoin() {
        if (status == GameRoomStatus.PLAYING) {
            throw new CoreException(ErrorType.GAME_ALREADY_PLAYING);
        }
    }

    public void leave(String player) {
        players.remove(player);
    }

    public List<String> getRoomPlayers() {
        return players.getPlayers();
    }

    public void start(String nickname) {
        validateStart(nickname);
        status = GameRoomStatus.PLAYING;
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

    public void end() {
        status = GameRoomStatus.WAITING;
    }

    public boolean isEmpty() {
        return players.getCurrentCount() == 0;
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

    public static GameRoom create(String title, Game game, List<String> initialPlayers, int maxPlayer, String host) {
        return new GameRoom(title, game, new GamePlayers(initialPlayers, maxPlayer, host));
    }

    public boolean isIdle(Instant threshold) {
        return lastActivityTime.isBefore(threshold);
    }

    public void touch() {
        lastActivityTime = Instant.now();
    }
}
