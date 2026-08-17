package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.storage.IntegrationTest;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class HangmanWordReaderTest {

    @Autowired
    private HangmanWordReader hangmanWordReader;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ParameterizedTest
    @CsvSource({"1, 536", "2, 673", "3, 1157", "4, 1302"})
    @DisplayName("난이도별 단어가 모두 적재되어 있다.")
    void seedsEveryWord(int difficulty, int expectedCount) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from hangman_word where difficulty = ?", Integer.class, difficulty);

        assertThat(count).isEqualTo(expectedCount);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    @DisplayName("난이도마다 단어를 뽑는다.")
    void readsWordForEveryDifficulty(int difficulty) {
        HangmanWord word = hangmanWordReader.findRandomByDifficulty(difficulty);

        assertThat(word.id()).isNotNull();
        assertThat(word.value()).isNotBlank();
        assertThat(word.difficulty()).isEqualTo(difficulty);
    }

    @Test
    @DisplayName("단어가 없는 난이도는 예외를 던진다.")
    void rejectsUnknownDifficulty() {
        assertThatThrownBy(() -> hangmanWordReader.findRandomByDifficulty(99))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.HANGMAN_WORD_FETCH_FAILED);
    }
}
