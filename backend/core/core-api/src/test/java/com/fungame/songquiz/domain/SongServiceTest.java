package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.SongStore;
import com.fungame.songquiz.support.extern.YoutubeScraper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @InjectMocks
    private SongService songService;

    @Mock
    private SongStore songStore;

    @Mock
    private YoutubeScraper youtubeScraper;

    @Test
    @DisplayName("정확한 제목과 발매일로 곡 존재 여부를 확인한다.")
    void existSongQuiz_withExactTitleAndReleaseDate() {
        // given
        String title = "밤편지";
        LocalDate releaseDate = LocalDate.of(2017, 3, 24);
        given(songStore.existsByTitleContainingAndReleaseDate(title, releaseDate)).willReturn(true);

        // when
        boolean result = songService.existSongQuiz(title, releaseDate);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("제목의 일부와 발매일로 곡 존재 여부를 확인한다 (Containing).")
    void existSongQuiz_withPartialTitleAndReleaseDate() {
        // given
        String partialTitle = "밤";
        LocalDate releaseDate = LocalDate.of(2017, 3, 24);
        given(songStore.existsByTitleContainingAndReleaseDate(partialTitle, releaseDate)).willReturn(true);

        // when
        boolean result = songService.existSongQuiz(partialTitle, releaseDate);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 제목으로 검색 시 false를 반환한다.")
    void existSongQuiz_withNonExistentTitle() {
        // given
        String title = "없는노래";
        LocalDate releaseDate = LocalDate.of(2024, 1, 1);
        given(songStore.existsByTitleContainingAndReleaseDate(title, releaseDate)).willReturn(false);

        // when
        boolean result = songService.existSongQuiz(title, releaseDate);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("발매일이 null인 경우 제목으로만 존재 여부를 확인한다.")
    void existSongQuiz_withNullReleaseDate() {
        // given
        String title = "밤편지";
        given(songStore.existsByTitleContaining(title)).willReturn(true);

        // when
        boolean result = songService.existSongQuiz(title, null);

        // then
        assertThat(result).isTrue();
    }
}
