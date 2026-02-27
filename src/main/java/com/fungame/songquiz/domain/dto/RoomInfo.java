package com.fungame.songquiz.domain.dto;

import com.fungame.songquiz.storage.GameRoom;
import com.fungame.songquiz.storage.GameRoomStatus;

public record RoomInfo(
        String roomId,
        String title,
        String hostName,
        GameRoomStatus status,
        int maxPlayers,
        int currentPlayers
) {

    public static RoomInfo from(GameRoom gameRoom) {
        return new RoomInfo(
                gameRoom.getId(),
                gameRoom.getTitle(),
                gameRoom.getHostName(),
                gameRoom.getStatus(),
                gameRoom.getMaxPlayers(),
                gameRoom.getPlayerNames().size()
        );
    }
}
