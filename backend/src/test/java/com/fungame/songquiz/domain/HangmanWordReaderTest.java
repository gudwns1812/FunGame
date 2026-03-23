package com.fungame.songquiz.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HangmanWordReaderTest {

    @Mock
    private HangmanWordProvider wordProvider;

    @InjectMocks
    private HangmanWordReader wordReader;

    @Test
    @DisplayName("단어 공급자로부터 받은 단어로 행맨 게임을 생성한다.")
    void create_game_with_provided_word() {
        // Given
        String expectedWord = "BANANA";
        int difficulty = 3;
        given(wordProvider.getWord(difficulty)).willReturn(expectedWord);

        // When
        HangmanGame game = wordReader.create(difficulty);

        // Then
        assertThat(game).isNotNull();
        assertThat(game.getAnswer().data()).containsExactly(expectedWord);
    }
}
