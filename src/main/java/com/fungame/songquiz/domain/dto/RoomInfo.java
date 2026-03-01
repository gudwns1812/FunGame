package com.fungame.songquiz.domain.dto;

import com.fungame.songquiz.domain.GameRoomStatus;
import com.fungame.songquiz.storage.GameRoomEntity;

public record RoomInfo(
        String roomId,
        String title,
        String hostName,
        GameRoomStatus status,
        int maxPlayers,
        int currentPlayers
) {

    public static RoomInfo from(GameRoomEntity gameRoomEntity) {
        return new RoomInfo(
                gameRoomEntity.getId(),
                gameRoomEntity.getTitle(),
                gameRoomEntity.getHostName(),
                gameRoomEntity.getStatus(),
                gameRoomEntity.getMaxPlayers(),
                gameRoomEntity.getPlayerNames().size()
        );
    }
}
