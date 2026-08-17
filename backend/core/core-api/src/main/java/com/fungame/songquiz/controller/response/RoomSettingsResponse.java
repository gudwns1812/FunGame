package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.RoomSettings;
import com.fungame.songquiz.domain.room.RoomStateInfo;
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

    public static RoomSettingsResponse from(RoomStateInfo state) {
        RoomSettings roomSettings = state.settings();
        GamePlayer host = state.host();

        return new RoomSettingsResponse(
                roomSettings.title(),
                roomSettings.gameType(),
                roomSettings.maxPlayers(),
                roomSettings.category(),
                roomSettings.totalRound(),
                roomSettings.difficulty(),
                roomSettings.csDifficulty(),
                host.memberId(),
                host.nickname());
    }
}
