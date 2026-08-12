package com.fungame.songquiz.domain.dto;

import com.fungame.songquiz.domain.GameRoom;
import com.fungame.songquiz.domain.GameRoomStatus;
import com.fungame.songquiz.domain.StoredRoom;

public record RoomInfo(
        Long roomId,
        String title,
        String hostName,
        GameRoomStatus status,
        int maxPlayers,
        int currentPlayers
) {

    public static RoomInfo from(Long roomId, GameRoom room) {
        return new RoomInfo(
                roomId,
                room.getTitle(),
                room.getPlayers().getHost(),
                room.getStatus(),
                room.getSettings().maxPlayers(),
                room.getPlayerCount()
        );
    }

    public static RoomInfo of(StoredRoom stored, int connectedPlayers) {
        return new RoomInfo(
                stored.roomId(),
                stored.settings().title(),
                stored.host(),
                stored.status(),
                stored.settings().maxPlayers(),
                connectedPlayers
        );
    }
}
