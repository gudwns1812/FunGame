package com.fungame.songquiz.domain.session;

import java.util.List;

public record GameOutcome(List<PlayerOutcome> players) {

    private static final int MIN_RECORDABLE_PLAYERS = 2;

    public boolean isRecordable() {
        return startedPlayerCount() >= MIN_RECORDABLE_PLAYERS;
    }

    public int startedPlayerCount() {
        return players.size();
    }
}
