package com.fungame.songquiz.domain.dto;

import com.fungame.songquiz.storage.GameRoom;

public record RoomInfo(
        String roomId,
        String title,
        String hostName,
        int maxPlayers,
        int currentPlayers
) {

    public static RoomInfo from(GameRoom gameRoom) {
        return new RoomInfo(gameRoom.getId(), gameRoom.getTitle(), gameRoom.getHostName(), gameRoom.getMaxPlayers(),gameRoom.getPlayerNames().size());
    }
}
