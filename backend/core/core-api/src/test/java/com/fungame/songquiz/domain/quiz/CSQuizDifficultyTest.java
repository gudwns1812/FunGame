package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.CSQuizDifficulty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CSQuizDifficultyTest {

    @Test
    @DisplayName("EASY 는 자기 자신만 후보로 삼는다.")
    void easyCoversOnlyItself() {
        assertThat(CSQuizDifficulty.EASY.andEasier())
                .containsExactly(CSQuizDifficulty.EASY);
    }

    @Test
    @DisplayName("NORMAL 은 EASY 까지 후보로 삼는다.")
    void normalCoversEasy() {
        assertThat(CSQuizDifficulty.NORMAL.andEasier())
                .containsExactly(CSQuizDifficulty.EASY, CSQuizDifficulty.NORMAL);
    }

    @Test
    @DisplayName("HARD 는 모든 난이도를 후보로 삼는다.")
    void hardCoversEverything() {
        assertThat(CSQuizDifficulty.HARD.andEasier())
                .containsExactly(CSQuizDifficulty.EASY, CSQuizDifficulty.NORMAL, CSQuizDifficulty.HARD);
    }
}
