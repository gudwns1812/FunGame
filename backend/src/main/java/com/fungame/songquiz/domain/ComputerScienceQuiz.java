package com.fungame.songquiz.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;

@Getter
public class ComputerScienceQuiz {

    private final String field;
    private final String question;
    private final Set<String> answers;
    private final String explain;
    private final CSQuizDifficulty difficulty;

    private ComputerScienceQuiz(String field, String question, Set<String> answers, String explain,
                                CSQuizDifficulty difficulty) {
        this.field = field;
        this.question = question;
        this.answers = answers;
        this.explain = explain;
        this.difficulty = difficulty;
    }

    public String getAnswer() {
        return String.join(",", answers);
    }

    public boolean isCorrect(String answer) {
        return answers.contains(answer);
    }

    public static ComputerScienceQuiz of(String field, String question, List<String> answers, String explain,
                                         CSQuizDifficulty difficulty) {
        return new ComputerScienceQuiz(field, question, new HashSet<>(answers), explain, difficulty);
    }
}
