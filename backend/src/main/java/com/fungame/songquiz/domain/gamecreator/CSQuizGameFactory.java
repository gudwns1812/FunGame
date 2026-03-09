package com.fungame.songquiz.domain.gamecreator;

import com.fungame.songquiz.domain.*;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CSQuizGameFactory implements GameFactory {

    private final ComputerScienceQuizReader csQuizReader;

    @Override
    public GameType getSupportedType() {
        return GameType.CS;
    }

    @Override
    public Game create(GameCreateInfo info) {
        if (!(info instanceof CsQuizGameCreateInfo(int totalRound))) {
            throw new CoreException(ErrorType.GAME_NOT_FOUND);
        }

        List<ComputerScienceQuiz> computerScienceQuizs = csQuizReader.getRandomCSQuizWithCount(totalRound);
        return new ComputerScienceQuizGame(computerScienceQuizs);
    }
}
