package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.CSQuizDifficulty;
import java.util.List;

public record CsQuestionSnapshot(
        Long id,
        String field,
        String question,
        List<String> answers,
        String explain,
        CSQuizDifficulty difficulty
) {

    static CsQuestionSnapshot from(CsQuestion question) {
        return new CsQuestionSnapshot(question.getId(), question.getField(), question.getQuestion(),
                List.copyOf(question.getAnswers()), question.getExplain(), question.getDifficulty());
    }

    CsQuestion toQuestion() {
        return CsQuestion.of(id, field, question, answers, explain, difficulty);
    }
}
