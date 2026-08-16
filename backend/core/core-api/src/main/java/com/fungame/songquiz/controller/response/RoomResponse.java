package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.room.RoomInfo;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.GameRoomStatus;
import com.fungame.songquiz.enums.GameType;

import java.util.List;

public record RoomResponse(
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

    public static RoomResponse from(RoomInfo room) {
        return new RoomResponse(
                room.roomId(),
                room.title(),
                room.hostMemberId(),
                room.hostNickname(),
                room.status(),
                room.maxPlayers(),
                room.currentPlayers(),
                room.gameType(),
                room.csDifficulty());
    }

    public static List<RoomResponse> listFrom(List<RoomInfo> rooms) {
        return rooms.stream()
                .map(RoomResponse::from)
                .toList();
    }
}
