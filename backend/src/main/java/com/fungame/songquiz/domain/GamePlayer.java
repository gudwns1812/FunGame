package com.fungame.songquiz.domain;

public record GamePlayer(
        String name,
        boolean isReady
) {

    public boolean hasName(String player) {
        return name.equals(player);
    }

    public static GamePlayer createNewPlayer(String name) {
        return new GamePlayer(name, false);
    }

    public GamePlayer ready() {
        return new GamePlayer(this.name, true);
    }
}
