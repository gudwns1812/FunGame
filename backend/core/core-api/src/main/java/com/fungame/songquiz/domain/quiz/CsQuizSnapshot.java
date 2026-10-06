package com.fungame.songquiz.domain.quiz;

import java.util.List;

public record CsQuizSnapshot(
        List<CsQuestionSnapshot> questions,
        int currentIdx,
        boolean roundProcessing
) implements QuizSnapshot {

    @Override
    public Quiz restore() {
        return CsQuiz.restore(this);
    }
}
