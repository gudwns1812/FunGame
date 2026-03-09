package com.fungame.songquiz.domain;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class GamePlayers {
    private final List<String> players;
    @Getter
    private final int maxPlayer;
    @Getter
    private String host;

    public GamePlayers(List<String> players, int maxPlayer, String host) {
        this.players = new ArrayList<>(players);
        this.maxPlayer = maxPlayer;
        this.host = host;
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
        if (!players.contains(player)) {
            return;
        }
        players.remove(player);

        if (player.equals(host) && !players.isEmpty()) {
            delegateHost();
        }
    }

    private void delegateHost() {
        if (!players.isEmpty()) {
            host = players.get(0);
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
