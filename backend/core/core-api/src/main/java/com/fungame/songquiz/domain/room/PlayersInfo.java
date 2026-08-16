package com.fungame.songquiz.domain.room;

import java.util.List;

public record PlayersInfo(
        List<GamePlayer> players,
        GamePlayer host
) {

    public static PlayersInfo from(GameRoom room) {
        return new PlayersInfo(room.getRoomPlayers(), room.getHost());
    }
}
