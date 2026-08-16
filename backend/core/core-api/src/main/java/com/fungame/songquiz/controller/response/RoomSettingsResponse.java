package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.room.RoomSettingsInfo;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameType;

public record RoomSettingsResponse(
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

    public static RoomSettingsResponse from(RoomSettingsInfo settings) {
        return new RoomSettingsResponse(
                settings.title(),
                settings.gameType(),
                settings.maxPlayers(),
                settings.category(),
                settings.totalRound(),
                settings.difficulty(),
                settings.csDifficulty(),
                settings.hostMemberId(),
                settings.hostNickname());
    }
}
