package com.fungame.songquiz.domain;

import java.util.List;
import lombok.Getter;

@Getter
public class GameRoom {
    private final String title;
    private final Game game;
    private final GameRank rank;
    private final GameRoomStatus status;
    private final GamePlayers players;

    private GameRoom(String title, Game game, GameRank rank, GamePlayers players) {
        this.title = title;
        this.game = game;
        this.rank = rank;
        this.players = players;
        this.status = GameRoomStatus.WAITING;
    }

    public boolean submitAnswer(String player, String answer) {
        if (!game.isCorrect(answer)) {
            return false;
        }
        rank.updatePoint(player);
        return true;
    }

    public int addPlayer(String player) {
        players.add(player);
        rank.addPlayer(player);
        return players.getCurrentCount();
    }

    public void removePlayer(String player) {
        players.remove(player);
        rank.removePlayer(player);
    }

    public List<PlayerScore> getRank() {
        return rank.getPlayerScores();
    }

    public List<String> getRoomPlayers() {
        return players.getPlayers();
    }

    public static GameRoom create(String title, Game game, List<String> initialPlayers, int maxPlayer, String host) {
        return new GameRoom(title, game, new GameRank(initialPlayers),
                new GamePlayers(initialPlayers, maxPlayer, host));
    }
}
