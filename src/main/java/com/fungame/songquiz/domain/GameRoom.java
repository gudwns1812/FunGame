package com.fungame.songquiz.domain;

import java.util.List;

public class GameRoom {
    private final Game game;
    private final GameRank rank;
    private final GameRoomStatus status;
    private final GamePlayers players; // 일급 컬렉션 적용

    private GameRoom(Game game, GameRank rank, GamePlayers players) {
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

    // 팩토리 메서드들도 한결 깔끔해집니다
    public static GameRoom create(Game game, List<String> initialPlayers, int maxPlayer, String host) {
        return new GameRoom(game, new GameRank(initialPlayers), new GamePlayers(initialPlayers, maxPlayer, host));
    }
}
