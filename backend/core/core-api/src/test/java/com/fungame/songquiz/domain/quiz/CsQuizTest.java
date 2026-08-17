package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.CSQuizDifficulty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsQuizTest {

    private static final long FIRST_QUESTION_ID = 31L;
    private static final long SECOND_QUESTION_ID = 32L;

    private final CsQuiz quiz = new CsQuiz(List.of(
            storedQuestion(FIRST_QUESTION_ID, "첫 질문"),
            storedQuestion(SECOND_QUESTION_ID, "둘째 질문")));

    @Test
    @DisplayName("라운드가 시작되기 전에는 가리킬 문제가 없다.")
    void hasNoContentIdBeforeFirstRound() {
        assertThat(quiz.getCurrentContentId()).isNull();
    }

    @Test
    @DisplayName("라운드가 시작되면 그 라운드 문제의 식별자를 돌려준다.")
    void tellsContentIdOfCurrentRound() {
        quiz.startRound();

        assertThat(quiz.getCurrentContentId()).isEqualTo(FIRST_QUESTION_ID);
    }

    @Test
    @DisplayName("다음 라운드로 넘어가면 다음 문제의 식별자를 돌려준다.")
    void followsRoundToNextQuestion() {
        quiz.startRound();
        quiz.startRound();

        assertThat(quiz.getCurrentContentId()).isEqualTo(SECOND_QUESTION_ID);
    }

    @Test
    @DisplayName("마지막 라운드를 넘겨 버린 뒤에는 가리킬 문제가 없다.")
    void hasNoContentIdPastLastRound() {
        quiz.startRound();
        quiz.startRound();
        quiz.startRound();

        assertThat(quiz.getCurrentContentId()).isNull();
    }

    private static CsQuestion storedQuestion(long id, String question) {
        return CsQuestion.of(id, "네트워크", question, List.of("답"), "해설", CSQuizDifficulty.NORMAL);
    }
}
