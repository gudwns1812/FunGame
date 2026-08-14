package com.fungame.songquiz.domain.dto;

import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.domain.GamePlayers;
import com.fungame.songquiz.domain.GameRoom;
import com.fungame.songquiz.enums.GameRoomStatus;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.domain.StoredRoom;

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

    public static RoomInfo of(StoredRoom stored, int connectedPlayers) {
        return new RoomInfo(
                stored.roomId(),
                stored.settings().title(),
                stored.hostId(),
                stored.hostNickname(),
                stored.status(),
                stored.settings().maxPlayers(),
                connectedPlayers,
                stored.settings().gameType(),
                stored.settings().csDifficulty()
        );
    }
}
