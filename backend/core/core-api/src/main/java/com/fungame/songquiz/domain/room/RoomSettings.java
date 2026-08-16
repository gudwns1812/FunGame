package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.domain.quiz.CsQuizCreateInfo;
import com.fungame.songquiz.domain.quiz.QuizCreateInfo;
import com.fungame.songquiz.domain.quiz.HangmanQuizCreateInfo;
import com.fungame.songquiz.domain.quiz.SongQuizCreateInfo;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameType;

public record RoomSettings(
        GameType gameType,
        String title,
        int maxPlayers,
        Category category,
        int totalRound,
        int difficulty,
        CSQuizDifficulty csDifficulty
) {

    public QuizCreateInfo toQuizCreateInfo() {
        return switch (gameType) {
            case SONG -> new SongQuizCreateInfo(category, totalRound);
            case CS -> new CsQuizCreateInfo(totalRound, csDifficulty);
            case HANGMAN -> new HangmanQuizCreateInfo(difficulty);
            case NONE -> null;
        };
    }

    public RoomSettings changeTo(
            GameType newGameType,
            int newMaxPlayers,
            Category newCategory,
            int newTotalRound,
            int newDifficulty,
            CSQuizDifficulty newCsDifficulty) {
        return new RoomSettings(newGameType, title, newMaxPlayers, newCategory, newTotalRound, newDifficulty, newCsDifficulty);
    }
}
