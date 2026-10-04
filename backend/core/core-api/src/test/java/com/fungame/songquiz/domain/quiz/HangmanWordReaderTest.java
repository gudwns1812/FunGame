package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.support.ApiIntegrationTest;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HangmanWordReaderTest extends ApiIntegrationTest {

    private static final int EASY = 1;
    private static final int HARD = 2;
    private static final String EASY_WORD = "사과";
    private static final String HARD_WORD = "코끼리";

    @Autowired
    private HangmanWordReader hangmanWordReader;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedWords() {
        insert(EASY_WORD, EASY);
        insert(HARD_WORD, HARD);
    }

    @Test
    @DisplayName("요청한 난이도의 단어만 뽑는다.")
    void readsWordOfRequestedDifficulty() {
        HangmanWord word = hangmanWordReader.findRandomByDifficulty(HARD);

        assertThat(word.id()).isNotNull();
        assertThat(word.value()).isEqualTo(HARD_WORD);
        assertThat(word.difficulty()).isEqualTo(HARD);
    }

    @Test
    @DisplayName("같은 난이도에 여러 단어가 있으면 그중 하나를 뽑는다.")
    void readsOneOfManyWords() {
        insert("바나나", EASY);

        HangmanWord word = hangmanWordReader.findRandomByDifficulty(EASY);

        assertThat(word.value()).isIn(EASY_WORD, "바나나");
    }

    @Test
    @DisplayName("단어가 없는 난이도는 예외를 던진다.")
    void rejectsDifficultyWithoutWords() {
        assertThatThrownBy(() -> hangmanWordReader.findRandomByDifficulty(99))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.HANGMAN_WORD_FETCH_FAILED);
    }

    private void insert(String word, int difficulty) {
        jdbcTemplate.update(
                "insert into hangman_word (word, difficulty) values (?, ?)", word, difficulty);
    }
}
