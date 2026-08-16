package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CsQuizFactory implements QuizFactory {

    private final CsQuestionReader csQuizReader;

    @Override
    public GameType getSupportedType() {
        return GameType.CS;
    }

    @Override
    public Quiz create(QuizCreateInfo info) {
        if (!(info instanceof CsQuizCreateInfo(int totalRound, CSQuizDifficulty difficulty))) {
            throw new CoreException(ErrorType.GAME_NOT_FOUND);
        }

        List<CsQuestion> computerScienceQuizs = csQuizReader.getRandomCSQuizWithCount(totalRound, difficulty);
        return new CsQuiz(computerScienceQuizs);
    }
}
