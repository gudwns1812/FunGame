package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.GameRoomStatus;
import com.fungame.songquiz.enums.GameType;

public record RoomInfo(
        Long roomId,
        String title,
        Long hostMemberId,
        String hostNickname,
        GameRoomStatus status,
        int maxPlayers,
        int currentPlayers,
        GameType gameType,
        CSQuizDifficulty csDifficulty
) {

    public static RoomInfo from(GameRoom room) {
        return new RoomInfo(
                room.getRoomId(),
                room.getTitle(),
                room.getHostId(),
                room.hostNickname(),
                room.getStatus(),
                room.getSettings().maxPlayers(),
                room.getPlayerCount(),
                room.getSettings().gameType(),
                room.getSettings().csDifficulty()
        );
    }

    public RoomInfo withConnectedPlayers(int connectedPlayers) {
        return new RoomInfo(
                roomId,
                title,
                hostMemberId,
                hostNickname,
                status,
                maxPlayers,
                connectedPlayers,
                gameType,
                csDifficulty
        );
    }
}
