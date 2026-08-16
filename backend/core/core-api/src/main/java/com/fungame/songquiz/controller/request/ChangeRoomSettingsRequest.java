package com.fungame.songquiz.controller.request;

import com.fungame.songquiz.domain.room.RoomSettings;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeRoomSettingsRequest {
    private GameType gameType;
    private int maxPlayers;
    private Category category;
    private int totalRound;
    private int difficulty;
    private CSQuizDifficulty csDifficulty;

    public RoomSettings applyTo(RoomSettings current) {
        return current.changeTo(gameType, maxPlayers, category, totalRound, difficulty, csDifficultyOr(current.csDifficulty()));
    }

    private CSQuizDifficulty csDifficultyOr(CSQuizDifficulty unchanged) {
        return csDifficulty == null ? unchanged : csDifficulty;
    }
}
