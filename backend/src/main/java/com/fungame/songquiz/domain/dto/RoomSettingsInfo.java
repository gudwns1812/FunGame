package com.fungame.songquiz.domain.dto;

import com.fungame.songquiz.domain.Category;
import com.fungame.songquiz.domain.GameRoom;
import com.fungame.songquiz.domain.GameType;
import com.fungame.songquiz.domain.RoomSettings;

public record RoomSettingsInfo(
        String title,
        GameType gameType,
        int maxPlayers,
        Category category,
        int totalRound,
        int difficulty,
        String host
) {

    public RoomSettings toSettings() {
        return new RoomSettings(gameType, title, maxPlayers, category, totalRound, difficulty);
    }

    public static RoomSettingsInfo from(GameRoom gameRoom) {
        RoomSettings settings = gameRoom.getSettings();

        return new RoomSettingsInfo(
                settings.title(),
                settings.gameType(),
                settings.maxPlayers(),
                settings.category(),
                settings.totalRound(),
                settings.difficulty(),
                gameRoom.getPlayers().getHost()
        );
    }
}
