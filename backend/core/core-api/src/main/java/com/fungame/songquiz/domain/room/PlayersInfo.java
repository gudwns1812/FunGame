package com.fungame.songquiz.domain.room;


import java.util.List;

public record PlayersInfo(
        List<GamePlayerInfo> players,
        Long hostMemberId,
        String hostNickname
) {
    public static PlayersInfo from(GameRoom room) {
        GamePlayers gamePlayers = room.getPlayers();
        Long hostMemberId = gamePlayers.getHost();

        return new PlayersInfo(
                gamePlayers.getPlayersWithReadyStatus(),
                hostMemberId,
                gamePlayers.nicknameOf(hostMemberId)
        );
    }
}
