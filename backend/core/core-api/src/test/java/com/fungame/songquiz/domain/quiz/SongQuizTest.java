package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SongQuizTest {

    private static final long FIRST_SONG_ID = 11L;
    private static final long SECOND_SONG_ID = 22L;

    private final SongQuiz quiz = new SongQuiz(
            List.of(storedSong(FIRST_SONG_ID, "첫곡"), storedSong(SECOND_SONG_ID, "둘째곡")),
            Category.KPOP);

    @Test
    @DisplayName("라운드가 시작되기 전에는 가리킬 문제가 없다.")
    void hasNoContentIdBeforeFirstRound() {
        assertThat(quiz.getCurrentContentId()).isNull();
    }

    @Test
    @DisplayName("라운드가 시작되면 그 라운드 곡의 식별자를 돌려준다.")
    void tellsContentIdOfCurrentRound() {
        quiz.startRound();

        assertThat(quiz.getCurrentContentId()).isEqualTo(FIRST_SONG_ID);
    }

    @Test
    @DisplayName("다음 라운드로 넘어가면 다음 곡의 식별자를 돌려준다.")
    void followsRoundToNextSong() {
        quiz.startRound();
        quiz.startRound();

        assertThat(quiz.getCurrentContentId()).isEqualTo(SECOND_SONG_ID);
    }

    @Test
    @DisplayName("마지막 라운드를 넘겨 버린 뒤에는 가리킬 문제가 없다.")
    void hasNoContentIdPastLastRound() {
        quiz.startRound();
        quiz.startRound();
        quiz.startRound();

        assertThat(quiz.getCurrentContentId()).isNull();
    }

    private static Song storedSong(long id, String title) {
        return Song.stored(id, title, "가수", List.of(Category.KPOP), LocalDate.of(2020, 1, 1),
                "youtube.com/" + id, 30, List.of(), "");
    }
}
