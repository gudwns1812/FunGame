package com.fungame.songquiz.domain;

import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.storage.SongEntity;
import com.fungame.songquiz.storage.SongRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SongReaderTest {

    private static final String TITLE = "밤편지";
    private static final String SINGER = "아이유";
    private static final LocalDate RELEASE_DATE = LocalDate.of(2017, 3, 24);
    private static final String VIDEO_LINK = "https://youtu.be/BzYnNdJhZQw";
    private static final int PLAY_SECONDS = 30;
    private static final String HINT = "세 글자";

    @Mock
    private SongRepository songRepository;

    @InjectMocks
    private SongReader songReader;

    private static SongEntity entity() {
        return SongEntity.builder()
                .id(1L)
                .title(TITLE)
                .singer(SINGER)
                .categories(List.of(Category.KPOP, Category.BALLAD))
                .releaseDate(RELEASE_DATE)
                .videoLink(VIDEO_LINK)
                .playSeconds(PLAY_SECONDS)
                .answers(List.of("밤 편지"))
                .hint(HINT)
                .build();
    }

    private static void assertMappedFrom(Song song) {
        assertThat(song.getTitle()).isEqualTo(TITLE);
        assertThat(song.getSinger()).isEqualTo(SINGER);
        assertThat(song.getCategories()).containsExactly(Category.KPOP, Category.BALLAD);
        assertThat(song.getReleaseDate()).isEqualTo(RELEASE_DATE);
        assertThat(song.getLink()).isEqualTo(VIDEO_LINK);
        assertThat(song.getPlaySeconds()).isEqualTo(PLAY_SECONDS);
        assertThat(song.getHint()).isEqualTo(HINT);
        assertThat(song.getAnswers()).containsExactlyInAnyOrder("밤 편지", TITLE);
    }

    @Test
    @DisplayName("id로 조회한 엔티티의 모든 필드를 도메인으로 옮긴다.")
    void mapsEveryFieldWhenFoundById() {
        given(songRepository.findById(1L)).willReturn(Optional.of(entity()));

        assertMappedFrom(songReader.findById(1L));
    }

    @Test
    @DisplayName("id로 찾지 못하면 null을 돌려준다.")
    void returnsNullWhenNotFoundById() {
        given(songRepository.findById(1L)).willReturn(Optional.empty());

        assertThat(songReader.findById(1L)).isNull();
    }

    @Test
    @DisplayName("개수로 조회한 엔티티의 모든 필드를 도메인으로 옮긴다.")
    void mapsEveryFieldWhenReadByCount() {
        given(songRepository.findRandomSongs(anyInt())).willReturn(List.of(entity()));

        List<Song> songs = songReader.findSongWithCount(1);

        assertThat(songs).hasSize(1);
        assertMappedFrom(songs.getFirst());
    }

    @Test
    @DisplayName("카테고리로 조회한 엔티티의 모든 필드를 도메인으로 옮긴다.")
    void mapsEveryFieldWhenReadByCategory() {
        given(songRepository.findRandomSongsByCategory(anyString(), anyInt())).willReturn(List.of(entity()));

        List<Song> songs = songReader.findSongByCategoryWithCount(Category.KPOP, 1);

        assertThat(songs).hasSize(1);
        assertMappedFrom(songs.getFirst());
    }

    @Test
    @DisplayName("카테고리는 JSON 문자열로 감싸서 조회 조건에 넘긴다.")
    void wrapsCategoryAsJsonString() {
        given(songRepository.findRandomSongsByCategory("\"KPOP\"", 5)).willReturn(List.of());

        assertThat(songReader.findSongByCategoryWithCount(Category.KPOP, 5)).isEmpty();
    }
}
