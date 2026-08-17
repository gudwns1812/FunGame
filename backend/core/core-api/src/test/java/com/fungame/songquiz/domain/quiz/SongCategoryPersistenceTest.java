package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.storage.IntegrationTest;
import com.fungame.songquiz.storage.SongEntity;
import com.fungame.songquiz.storage.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class SongCategoryPersistenceTest {

    private static final LocalDate RELEASE_DATE = LocalDate.of(2017, 3, 24);
    private static final int PLAY_SECONDS = 30;
    private static final int ENOUGH_TO_READ_ALL = 100;

    @Autowired
    private SongReader songReader;

    @Autowired
    private SongRepository songRepository;

    @BeforeEach
    void clearSongs() {
        songRepository.deleteAll();
    }

    @Test
    @DisplayName("저장한 곡의 카테고리를 그대로 다시 읽는다.")
    void restoresCategoriesOfSavedSong() {
        Long id = songRepository.save(song("밤편지", "아이유", Category.KPOP, Category.BALLAD)).getId();

        assertThat(songReader.findById(id).getCategories())
                .containsExactlyInAnyOrder(Category.KPOP, Category.BALLAD);
    }

    @Test
    @DisplayName("카테고리로 조회하면 그 카테고리를 가진 곡만 나온다.")
    void findsOnlySongsTaggedWithCategory() {
        songRepository.save(song("밤편지", "아이유", Category.KPOP, Category.BALLAD));
        songRepository.save(song("Shape of You", "에드 시런", Category.POP));

        List<Song> songs = songReader.findSongByCategoryWithCount(Category.BALLAD, ENOUGH_TO_READ_ALL);

        assertThat(songs).extracting(Song::getTitle).containsExactly("밤편지");
    }

    @Test
    @DisplayName("카테고리가 하나도 없는 곡은 카테고리 조회에 걸리지 않는다.")
    void skipsSongWithoutAnyCategory() {
        songRepository.save(song("무소속곡", "가수없음"));

        assertThat(songReader.findSongByCategoryWithCount(Category.KPOP, ENOUGH_TO_READ_ALL)).isEmpty();
    }

    @Test
    @DisplayName("곡을 지우면 카테고리도 함께 사라진다.")
    void removesCategoriesWithSong() {
        songRepository.save(song("밤편지", "아이유", Category.KPOP));

        songRepository.deleteAll();

        assertThat(songReader.findSongByCategoryWithCount(Category.KPOP, ENOUGH_TO_READ_ALL)).isEmpty();
    }

    private static SongEntity song(String title, String singer, Category... categories) {
        return SongEntity.builder()
                .title(title)
                .singer(singer)
                .categories(List.of(categories))
                .releaseDate(RELEASE_DATE)
                .videoLink("https://youtu.be/" + title)
                .playSeconds(PLAY_SECONDS)
                .answers(List.of(title))
                .hint("힌트")
                .build();
    }
}
