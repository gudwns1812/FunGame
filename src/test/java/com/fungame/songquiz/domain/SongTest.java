package com.fungame.songquiz.domain;

import java.time.LocalDate;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class SongTest {

    @Test
    void 채팅이_정답목록에_있으면_정답처리한다() {
        //given
        String input = "불장난";
        Song song = Song.of("불장난", "블랙핑크", Category.KPOP, LocalDate.of(2016, 5, 15), "youtube.com/blackpink", 30,
                List.of(input, "불 장 난"));
        //when
        boolean result = song.isCorrect(input);
        //then
        Assertions.assertThat(result).isTrue();
    }
}
