package com.fungame.songquiz.domain;

import java.time.LocalDate;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class SongTest {

    @Test
    void 채팅이_노래제목과_똑같으면_정답처리한다() {
        //given
        String input = "불장난";
        Song song = Song.of(input, "블랙핑크", Category.KPOP, LocalDate.of(2016, 5, 15), "youtube.com/blackpink", 30,
                List.of(input));
        //when
        boolean result = song.hasTitle(input);
        //then
        Assertions.assertThat(result).isTrue();
    }
}
