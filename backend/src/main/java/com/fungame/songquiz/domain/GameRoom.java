package com.fungame.songquiz.domain;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import java.util.List;
import lombok.Getter;

@Getter
public class GameRoom {
    private final String title;
    private final Game game;
    private final GamePlayers players;
    private GameRoomStatus status;

    private GameRoom(String title, Game game, GamePlayers players) {
        this.title = title;
        this.game = game;
        this.players = players;
        this.status = GameRoomStatus.WAITING;
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

    public static GameRoom create(String title, Game game, List<String> initialPlayers, int maxPlayer, String host) {
        return new GameRoom(title, game, new GamePlayers(initialPlayers, maxPlayer, host));
    }
}
