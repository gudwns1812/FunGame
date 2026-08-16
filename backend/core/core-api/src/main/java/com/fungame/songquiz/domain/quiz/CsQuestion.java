package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.CSQuizDifficulty;
import lombok.Getter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
public class CsQuestion {

    private final String field;
    private final String question;
    private final Set<String> answers;
    private final String explain;
    private final CSQuizDifficulty difficulty;

    private CsQuestion(String field, String question, Set<String> answers, String explain,
                                CSQuizDifficulty difficulty) {
        this.field = field;
        this.question = question;
        this.answers = answers;
        this.explain = explain;
        this.difficulty = difficulty;
    }

    /** 정답이 여러 개면 콤마 뒤에 공백을 둔다. 공백이 없으면 화면에서 줄바꿈되지 않고 한 줄로 뻗는다. */
    public String getAnswer() {
        return String.join(", ", answers);
    }

    public boolean isCorrect(String answer) {
        return answers.contains(answer);
    }

    public static CsQuestion of(String field, String question, List<String> answers, String explain,
                                         CSQuizDifficulty difficulty) {
        return new CsQuestion(field, question, new LinkedHashSet<>(answers), explain, difficulty);
    }
}
