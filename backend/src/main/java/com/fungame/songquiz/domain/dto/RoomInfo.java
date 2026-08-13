package com.fungame.songquiz.domain.dto;

import com.fungame.songquiz.domain.GamePlayers;
import com.fungame.songquiz.domain.GameRoom;
import com.fungame.songquiz.domain.GameRoomStatus;
import com.fungame.songquiz.domain.StoredRoom;

public record RoomInfo(
        Long roomId,
        String title,
        Long hostMemberId,
        String hostNickname,
        GameRoomStatus status,
        int maxPlayers,
        int currentPlayers
) {

    public static RoomInfo from(Long roomId, GameRoom room) {
        GamePlayers players = room.getPlayers();

        return new RoomInfo(
                roomId,
                room.getTitle(),
                players.getHost(),
                players.nicknameOf(players.getHost()),
                room.getStatus(),
                room.getSettings().maxPlayers(),
                room.getPlayerCount()
        );
    }

    public static RoomInfo of(StoredRoom stored, int connectedPlayers) {
        return new RoomInfo(
                stored.roomId(),
                stored.settings().title(),
                stored.hostId(),
                stored.hostNickname(),
                stored.status(),
                stored.settings().maxPlayers(),
                connectedPlayers
        );
    }
}
