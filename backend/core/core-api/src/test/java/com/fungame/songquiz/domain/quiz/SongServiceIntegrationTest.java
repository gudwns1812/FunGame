package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.support.ApiIntegrationTest;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.storage.SongEntity;
import com.fungame.songquiz.storage.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class SongServiceIntegrationTest extends ApiIntegrationTest {

    @Autowired
    private SongService songService;

    @Autowired
    private SongRepository songRepository;

    private final LocalDate releaseDate = LocalDate.of(2017, 3, 24);

    @BeforeEach
    void setUp() {
        songRepository.deleteAll();

        SongEntity song = SongEntity.builder()
                .title("밤편지")
                .singer("아이유")
                .categories(List.of(Category.KPOP, Category.BALLAD))
                .answers(List.of("밤편지", "through the night"))
                .releaseDate(releaseDate)
                .videoLink("https://www.youtube.com/watch?v=BzYnNdJhDMA")
                .playSeconds(180)
                .build();
        songRepository.save(song);
    }

    @Test
    @DisplayName("저장된 곡과 가수 · 제목이 같으면 아는 곡이다.")
    void knowsSavedSongBySingerAndTitle() {
        assertThat(songService.isKnown("아이유", "밤편지")).isTrue();
        assertThat(songService.isKnown("아이유", "좋은날")).isFalse();
    }

    @Test
    @DisplayName("새 곡은 한 번만 대기열에 들어가고, 그 뒤로는 아는 곡이 된다.")
    void enqueuesNewSongOnlyOnce() {
        Song song = Song.of("좋은날", "아이유", List.of(Category.KPOP), LocalDate.of(2010, 12, 9), null, 0,
                List.of(), "ㅈㅇㄴ");

        assertThat(songService.enqueueIfNew(song)).isTrue();
        assertThat(songService.enqueueIfNew(song)).isFalse();
        assertThat(songService.isKnown("아이유", "좋은날")).isTrue();
    }

    @Test
    @DisplayName("실제 DB에서 정확한 제목과 발매일로 곡 존재 여부를 확인한다.")
    void existSongQuiz_withExactTitleAndReleaseDate() {
        // when
        boolean exists = songService.existSongQuiz("밤편지", releaseDate);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("실제 DB에서 제목의 일부와 발매일로 곡 존재 여부를 확인한다 (Containing).")
    void existSongQuiz_withPartialTitleAndReleaseDate() {
        // when
        boolean exists = songService.existSongQuiz("밤", releaseDate);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("실제 DB에서 발매일이 다르면 다른 곡으로 취급한다.")
    void existSongQuiz_withDifferentReleaseDate() {
        // when
        boolean exists = songService.existSongQuiz("밤편지", LocalDate.of(2024, 1, 1));

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("실제 DB에서 존재하지 않는 제목으로 검색 시 false를 반환한다.")
    void existSongQuiz_withNonExistentTitle() {
        // when
        boolean exists = songService.existSongQuiz("좋은날", releaseDate);

        // then
        assertThat(exists).isFalse();
    }
}
