package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    private static final String TITLE = "밤편지";
    private static final LocalDate RELEASE_DATE = LocalDate.of(2017, 3, 24);

    @InjectMocks
    private SongService songService;

    @Mock
    private SongReader songReader;

    @Mock
    private SongScrapeRequestReader songScrapeRequestReader;

    @Mock
    private SongScrapeRequestWriter songScrapeRequestWriter;

    private static Song song() {
        return Song.of(TITLE, "아이유", List.of(Category.KPOP), RELEASE_DATE, null, 30, List.of("밤 편지"), "힌트");
    }

    @Test
    @DisplayName("곡 존재 여부는 저장된 곡에서 찾는다.")
    void existSongQuiz() {
        given(songReader.existsByTitleLike(TITLE, RELEASE_DATE)).willReturn(true);

        assertThat(songService.existSongQuiz(TITLE, RELEASE_DATE)).isTrue();
    }

    @Test
    @DisplayName("겹치는 곡이 없으면 스크랩 대기열에 넣는다.")
    void queuesNewSong() {
        given(songReader.existsSameSong(any())).willReturn(false);
        given(songScrapeRequestReader.existsSameSong(any())).willReturn(false);

        songService.createSongQuiz(song());

        verify(songScrapeRequestWriter).append(any());
    }

    @Test
    @DisplayName("이미 저장된 곡이면 대기열에 넣지 않고 거부한다.")
    void rejectsSongAlreadySaved() {
        given(songReader.existsSameSong(any())).willReturn(true);

        assertThatThrownBy(() -> songService.createSongQuiz(song()))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.QUIZ_DUPLICATE_ERROR);
        verify(songScrapeRequestWriter, never()).append(any());
    }

    @Test
    @DisplayName("겹치는 곡이 없으면 대기열에 넣고 참을 돌려준다.")
    void enqueuesNewSong() {
        given(songReader.existsSameSong(any())).willReturn(false);
        given(songScrapeRequestReader.existsSameSong(any())).willReturn(false);

        assertThat(songService.enqueueIfNew(song())).isTrue();
        verify(songScrapeRequestWriter).append(any());
    }

    @Test
    @DisplayName("겹치는 곡이 있으면 예외 없이 거짓을 돌려주고 넣지 않는다.")
    void skipsKnownSong() {
        given(songReader.existsSameSong(any())).willReturn(false);
        given(songScrapeRequestReader.existsSameSong(any())).willReturn(true);

        assertThat(songService.enqueueIfNew(song())).isFalse();
        verify(songScrapeRequestWriter, never()).append(any());
    }

    @Test
    @DisplayName("가수와 제목이 같은 곡이 저장됐거나 대기열에 있으면 아는 곡이다.")
    void knownBySingerAndTitle() {
        given(songReader.existsBySingerAndTitle("아이유", TITLE)).willReturn(false);
        given(songScrapeRequestReader.existsBySingerAndTitle("아이유", TITLE)).willReturn(true);

        assertThat(songService.isKnown("아이유", TITLE)).isTrue();
    }

    @Test
    @DisplayName("가수와 제목이 같은 곡이 어디에도 없으면 모르는 곡이다.")
    void unknownBySingerAndTitle() {
        given(songReader.existsBySingerAndTitle("아이유", TITLE)).willReturn(false);
        given(songScrapeRequestReader.existsBySingerAndTitle("아이유", TITLE)).willReturn(false);

        assertThat(songService.isKnown("아이유", TITLE)).isFalse();
    }

    @Test
    @DisplayName("이미 대기열에 있는 곡이면 거부한다.")
    void rejectsSongAlreadyQueued() {
        given(songReader.existsSameSong(any())).willReturn(false);
        given(songScrapeRequestReader.existsSameSong(any())).willReturn(true);

        assertThatThrownBy(() -> songService.createSongQuiz(song()))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.QUIZ_DUPLICATE_ERROR);
        verify(songScrapeRequestWriter, never()).append(any());
    }
}
