package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.domain.room.GamePlayer;
import java.util.List;

public record HangmanQuizSnapshot(
        Long wordId,
        String answer,
        List<Character> correctLetters,
        List<Character> wrongLetters,
        int remainingTries,
        int currentTurnIndex,
        List<GamePlayer> playerOrder,
        boolean roundProcessing
) implements QuizSnapshot {

    @Override
    public Quiz restore() {
        return HangmanQuiz.restore(this);
    }
}
