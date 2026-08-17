package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.enums.GameRoomStatus;

import java.util.List;

public record RoomStateInfo(
        Long roomId,
        long version,
        GameRoomStatus status,
        RoomSettings settings,
        List<GamePlayer> players,
        GamePlayer host
) {

    public static RoomStateInfo from(GameRoom room) {
        return new RoomStateInfo(
                room.getRoomId(),
                room.getVersion(),
                room.getStatus(),
                room.getSettings(),
                room.getRoomPlayers(),
                room.getHost());
    }
}
