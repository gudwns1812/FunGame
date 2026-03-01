package com.fungame.songquiz.domain;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class GamePlayers {
    private final List<String> players;
    private final int maxPlayer;
    private String host;

    public GamePlayers(List<String> players, int maxPlayer, String initialHost) {
        this.players = new ArrayList<>(players);
        this.maxPlayer = maxPlayer;
        this.host = initialHost;
    }

    public void add(String player) {
        if (isFull()) {
            throw new CoreException(ErrorType.GAME_ROOM_MAX_PLAYER_EXCEED);
        }
        if (players.contains(player)) {
            return;
        }

        players.add(player);
    }

    public void remove(String player) {
        if (players.isEmpty()) {
            throw new CoreException(ErrorType.GAME_ROOM_PLAYER_EMPTY);
        }
        players.remove(player);

        if (player.equals(host) && !players.isEmpty()) {
            this.host = players.getFirst();
        }
    }

    public boolean isFull() {
        return players.size() >= maxPlayer;
    }

    public List<String> getPlayers() {
        return List.copyOf(players);
    }

    public int getCurrentCount() {
        return players.size();
    }
}
