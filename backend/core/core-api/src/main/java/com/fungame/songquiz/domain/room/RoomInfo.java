package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.enums.GameRoomStatus;

public record RoomInfo(
        Long roomId,
        RoomSettings settings,
        GamePlayer host,
        GameRoomStatus status,
        int currentPlayers
) {

    public static RoomInfo from(GameRoom room) {
        return new RoomInfo(
                room.getRoomId(),
                room.getSettings(),
                room.getHost(),
                room.getStatus(),
                room.getPlayerCount());
    }
}
