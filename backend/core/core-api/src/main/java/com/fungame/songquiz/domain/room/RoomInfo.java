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

    public static RoomInfo from(Long roomId, GameRoom room) {
        GamePlayers players = room.getPlayers();

        return new RoomInfo(
                roomId,
                room.getTitle(),
                players.getHost(),
                players.nicknameOf(players.getHost()),
                room.getStatus(),
                room.getSettings().maxPlayers(),
                room.getPlayerCount(),
                room.getSettings().gameType(),
                room.getSettings().csDifficulty()
        );
    }

    public static RoomInfo of(StoredRoom stored) {
        return new RoomInfo(
                stored.roomId(),
                stored.settings().title(),
                stored.hostId(),
                stored.hostNickname(),
                stored.status(),
                stored.settings().maxPlayers(),
                stored.players().size(),
                stored.settings().gameType(),
                stored.settings().csDifficulty()
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
