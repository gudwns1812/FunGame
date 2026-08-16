package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.client.youtube.YoutubeScraper;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.storage.IntegrationTest;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@IntegrationTest
class SongScrapeFlowTest {

    private static final String VIDEO_ID = "BzYnNdJhZQw";
    private static final LocalDate RELEASE_DATE = LocalDate.of(2017, 3, 24);

    @Autowired
    private SongService songService;

    @Autowired
    private SongScrapeService songScrapeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private YoutubeScraper youtubeScraper;

    @BeforeEach
    void clearSongs() {
        jdbcTemplate.update("delete from song_scrape_request");
        jdbcTemplate.update("delete from song_entity");
    }

    private static Song song(String title, String singer, LocalDate releaseDate) {
        return Song.of(title, singer, List.of(Category.KPOP, Category.BALLAD), releaseDate,
                null, 30, List.of("다른 정답"), "힌트");
    }

    private int countIn(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }

    @Test
    @DisplayName("초안을 저장하면 대기 테이블에만 남고 유튜브를 부르지 않는다.")
    void queuesDraftWithoutCallingYoutube() {
        songService.createSongQuiz(song("밤편지", "아이유", RELEASE_DATE));

        assertThat(countIn("song_scrape_request")).isEqualTo(1);
        assertThat(countIn("song_entity")).isZero();
        verify(youtubeScraper, never()).findVideoId(anyString(), anyString());
    }

    @Test
    @DisplayName("가수와 제목이 같은 곡은 초안 저장에서 거부한다.")
    void rejectsSameSingerAndTitle() {
        songService.createSongQuiz(song("밤편지", "아이유", RELEASE_DATE));

        assertThatThrownBy(() -> songService.createSongQuiz(song("밤편지", "아이유", LocalDate.of(2020, 1, 1))))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.QUIZ_DUPLICATE_ERROR);
    }

    @Test
    @DisplayName("제목과 발매일이 같으면 가수가 달라도 초안 저장에서 거부한다.")
    void rejectsSameTitleAndReleaseDate() {
        songService.createSongQuiz(song("밤편지", "아이유", RELEASE_DATE));

        assertThatThrownBy(() -> songService.createSongQuiz(song("밤편지", "다른가수", RELEASE_DATE)))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.QUIZ_DUPLICATE_ERROR);
    }

    @Test
    @DisplayName("스크랩이 끝난 곡은 대기 테이블에서 빠지고 컬럼이 모두 채워진다.")
    void fillsPendingSongIntoSongEntity() {
        given(youtubeScraper.findVideoId(anyString(), anyString())).willReturn(Optional.of(VIDEO_ID));
        songService.createSongQuiz(song("밤편지", "아이유", RELEASE_DATE));

        songScrapeService.fillPendingSongs();

        assertThat(countIn("song_scrape_request")).isZero();
        Map<String, Object> row = jdbcTemplate.queryForMap("select * from song_entity");
        assertThat(row.get("title")).isEqualTo("밤편지");
        assertThat(row.get("singer")).isEqualTo("아이유");
        assertThat(row.get("video_link")).isEqualTo(VIDEO_ID);
        assertThat(row.get("play_seconds")).isEqualTo(30);
        assertThat(row.get("hint")).isEqualTo("힌트");
        assertThat(row.get("release_date").toString()).isEqualTo("2017-03-24");
    }

    @Test
    @DisplayName("카테고리와 정답은 엔티티가 읽을 수 있는 형식으로 저장된다.")
    void writesColumnsInConverterFormat() {
        given(youtubeScraper.findVideoId(anyString(), anyString())).willReturn(Optional.of(VIDEO_ID));
        songService.createSongQuiz(song("밤편지", "아이유", RELEASE_DATE));

        songScrapeService.fillPendingSongs();

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select categories, answers from song_entity");
        assertThat(row.get("categories").toString())
                .contains("KPOP")
                .contains("BALLAD");
        assertThat(row.get("answers").toString())
                .as("StringListConverter 는 콤마로 잇는다. Song.of 가 제목을 정답에 넣는다")
                .contains("다른 정답")
                .contains("밤편지")
                .contains(",");
    }

    @Test
    @DisplayName("스크랩 결과가 같은 영상이면 행이 하나만 남고 내용이 갱신된다.")
    void upsertsRowsSharingVideoLink() {
        given(youtubeScraper.findVideoId(anyString(), anyString())).willReturn(Optional.of(VIDEO_ID));
        songService.createSongQuiz(song("먼저올린곡", "가수하나", LocalDate.of(2020, 1, 1)));
        songScrapeService.fillPendingSongs();

        songService.createSongQuiz(song("나중올린곡", "가수둘", LocalDate.of(2021, 2, 2)));
        songScrapeService.fillPendingSongs();

        assertThat(countIn("song_entity"))
                .as("video_link unique 제약이 있어야 두 번째 저장이 갱신으로 처리된다")
                .isEqualTo(1);
        Map<String, Object> row = jdbcTemplate.queryForMap("select title, singer from song_entity");
        assertThat(row.get("title")).isEqualTo("나중올린곡");
        assertThat(row.get("singer")).isEqualTo("가수둘");
    }

    @Test
    @DisplayName("스크랩이 실패하면 대기 테이블에 남아 다음 회차에 다시 시도한다.")
    void keepsRequestWhenScrapeFails() {
        given(youtubeScraper.findVideoId(anyString(), anyString())).willReturn(Optional.empty());
        songService.createSongQuiz(song("밤편지", "아이유", RELEASE_DATE));

        songScrapeService.fillPendingSongs();

        assertThat(countIn("song_scrape_request")).isEqualTo(1);
        assertThat(countIn("song_entity")).isZero();

        given(youtubeScraper.findVideoId(anyString(), anyString())).willReturn(Optional.of(VIDEO_ID));
        songScrapeService.fillPendingSongs();

        assertThat(countIn("song_scrape_request")).isZero();
        assertThat(countIn("song_entity")).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 저장된 곡과 겹치면 초안 저장에서 거부한다.")
    void rejectsSongAlreadySaved() {
        given(youtubeScraper.findVideoId(anyString(), anyString())).willReturn(Optional.of(VIDEO_ID));
        songService.createSongQuiz(song("밤편지", "아이유", RELEASE_DATE));
        songScrapeService.fillPendingSongs();

        assertThatThrownBy(() -> songService.createSongQuiz(song("밤편지", "아이유", RELEASE_DATE)))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("type", ErrorType.QUIZ_DUPLICATE_ERROR);
        assertThat(countIn("song_scrape_request")).isZero();
    }
}
