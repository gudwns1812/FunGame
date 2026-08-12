package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.gamecreator.CsQuizGameCreateInfo;
import com.fungame.songquiz.domain.gamecreator.GameCreateInfo;
import com.fungame.songquiz.domain.gamecreator.HangmanGameCreateInfo;
import com.fungame.songquiz.domain.gamecreator.SongGameCreateInfo;

public record RoomSettings(
        GameType gameType,
        String title,
        int maxPlayers,
        Category category,
        int totalRound,
        int difficulty
) {

    public GameCreateInfo toGameCreateInfo() {
        return switch (gameType) {
            case SONG -> new SongGameCreateInfo(category, totalRound);
            case CS -> new CsQuizGameCreateInfo(totalRound);
            case HANGMAN -> new HangmanGameCreateInfo(difficulty);
            case NONE -> null;
        };
    }

    public RoomSettings changeTo(GameType newGameType, int newMaxPlayers, Category newCategory, int newTotalRound, int newDifficulty) {
        return new RoomSettings(newGameType, title, newMaxPlayers, newCategory, newTotalRound, newDifficulty);
    }
}
