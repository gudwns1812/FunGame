package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HangmanGameFactory implements GameFactory {

    private final HangmanWordReader hangmanWordReader;

    @Override
    public GameType getSupportedType() {
        return GameType.HANGMAN;
    }

    @Override
    public Game create(GameCreateInfo info) {
        if (!(info instanceof HangmanGameCreateInfo hangmanInfo)) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        int difficulty = hangmanInfo.getDifficulty();
        if (difficulty < 1 || difficulty > 5) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        return HangmanGame.create(hangmanWordReader.findRandomByDifficulty(difficulty).value());
    }
}
