package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.room.PlayersInfo;

import java.util.List;

public record RoomPlayersResponse(
        List<GamePlayerResponse> players,
        Long hostMemberId,
        String hostNickname
) {

    public static RoomPlayersResponse from(PlayersInfo players) {
        return new RoomPlayersResponse(
                GamePlayerResponse.listFrom(players.players()),
                players.hostMemberId(),
                players.hostNickname());
    }
}
