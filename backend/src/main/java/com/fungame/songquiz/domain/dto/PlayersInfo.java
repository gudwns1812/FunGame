package com.fungame.songquiz.domain.dto;

import com.fungame.songquiz.domain.GamePlayers;
import com.fungame.songquiz.domain.GameRoom;

import java.util.List;

public record PlayersInfo(
        List<GamePlayerInfo> players,
        String host
) {
    public static PlayersInfo from(GameRoom room) {
        GamePlayers gamePlayers = room.getPlayers();
        return new PlayersInfo(
                gamePlayers.getPlayersWithReadyStatus(),
                gamePlayers.getHost()
        );
    }
}
