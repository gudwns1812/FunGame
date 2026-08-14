package com.fungame.songquiz.domain.dto;

import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.domain.GameRoom;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.domain.RoomSettings;

public record RoomSettingsInfo(
        String title,
        GameType gameType,
        int maxPlayers,
        Category category,
        int totalRound,
        int difficulty,
        CSQuizDifficulty csDifficulty,
        Long hostMemberId,
        String hostNickname
) {

    public RoomSettings toSettings() {
        return new RoomSettings(gameType, title, maxPlayers, category, totalRound, difficulty, csDifficulty);
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
                settings.csDifficulty(),
                gameRoom.getPlayers().getHost(),
                gameRoom.getPlayers().nicknameOf(gameRoom.getPlayers().getHost())
        );
    }
}
