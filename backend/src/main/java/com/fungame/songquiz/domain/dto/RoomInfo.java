package com.fungame.songquiz.domain.dto;

import com.fungame.songquiz.domain.GameRoom;
import com.fungame.songquiz.domain.GameRoomStatus;

public record RoomInfo(
        Long roomId,
        String title,
        String hostName,
        GameRoomStatus status,
        int maxPlayers,
        int currentPlayers
) {

    public static RoomInfo from(Long id, GameRoom gameRoom) {
        return new RoomInfo(
                id,
                gameRoom.getTitle(),
                gameRoom.getPlayers().getHost(),
                gameRoom.getStatus(),
                gameRoom.getPlayers().getMaxPlayer(),
                gameRoom.getPlayers().getCurrentCount()
        );
    }
}
