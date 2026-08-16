package com.fungame.songquiz.domain.room;

public record RoomSettingsInfo(
        RoomSettings settings,
        GamePlayer host
) {

    public static RoomSettingsInfo from(GameRoom gameRoom) {
        return new RoomSettingsInfo(gameRoom.getSettings(), gameRoom.getHost());
    }
}
