package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.Category;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SongTest {

    private static final long SONG_ID = 42L;

    @Test
    void 채팅이_정답목록에_있으면_정답처리한다() {
        //given
        String input = "불장난";
        Song song = Song.of("불장난", "블랙핑크", List.of(Category.KPOP),
                LocalDate.of(2016, 5, 15),
                "youtube.com/blackpink", 30,
                List.of(input, "불 장 난"), "");
        //when
        boolean result = song.isCorrect(input);
        //then
        Assertions.assertThat(result).isTrue();
    }

    @Test
    @DisplayName("아직 저장되지 않은 곡은 식별자가 없다.")
    void hasNoIdBeforeStored() {
        assertThat(song().getId()).isNull();
    }

    @Test
    @DisplayName("저장된 곡은 어느 행에서 왔는지 알려준다.")
    void keepsIdOfStoredRow() {
        assertThat(storedSong().getId()).isEqualTo(SONG_ID);
    }

    @Test
    @DisplayName("저장된 곡도 제목을 정답으로 받아들인다.")
    void storedSongAcceptsTitleAsAnswer() {
        assertThat(storedSong().isCorrect("불장난")).isTrue();
    }

    private static Song song() {
        return Song.of("불장난", "블랙핑크", List.of(Category.KPOP), LocalDate.of(2016, 5, 15),
                "youtube.com/blackpink", 30, List.of("불 장 난"), "");
    }

    private static Song storedSong() {
        return Song.stored(SONG_ID, "불장난", "블랙핑크", List.of(Category.KPOP), LocalDate.of(2016, 5, 15),
                "youtube.com/blackpink", 30, List.of("불 장 난"), "");
    }
}
