package com.fungame.songquiz.controller.request;

import com.fungame.songquiz.domain.CSQuizDifficulty;
import com.fungame.songquiz.domain.Category;
import com.fungame.songquiz.domain.GameType;
import com.fungame.songquiz.domain.RoomSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoomRequest {
    private GameType gameType;
    private String title;
    private int maxPlayers;
    private Category category;
    private int totalRound;
    private int difficulty;
    private CSQuizDifficulty csDifficulty;

    public RoomSettings toRoomSettings() {
        return new RoomSettings(gameType, title, maxPlayers, category, totalRound, difficulty, csDifficultyOrAllQuestions());
    }

    private CSQuizDifficulty csDifficultyOrAllQuestions() {
        return csDifficulty == null ? CSQuizDifficulty.HARD : csDifficulty;
    }
}
