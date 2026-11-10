package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.client.gemini.GeminiReading;
import com.fungame.songquiz.client.gemini.GeminiTitle;
import com.fungame.songquiz.client.melon.MelonChartEntry;
import com.fungame.songquiz.client.melon.MelonSongDetail;
import com.fungame.songquiz.client.melon.MelonUnavailableException;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.support.ApiIntegrationTest;
import com.fungame.songquiz.support.TestEventCapture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MelonChartCollectorTest extends ApiIntegrationTest {

    private static final LocalDate RELEASE_DATE = LocalDate.of(2024, 2, 13);
    private static final MelonSongDetail BALLAD = new MelonSongDetail(RELEASE_DATE, List.of("발라드"));
    private static final long POPULAR = 60_000;

    @Autowired
    private MelonChartCollector collector;

    @Autowired
    private SongService songService;

    @Autowired
    private SongScrapeService songScrapeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestEventCapture eventCapture;

    @BeforeEach
    void clearSongs() {
        jdbcTemplate.update("delete from song_scrape_request");
        jdbcTemplate.update("delete from song_category");
        jdbcTemplate.update("delete from song_entity");
        eventCapture.clear();
    }

    @Test
    @DisplayName("좋아요 5만 이상인 한글 제목 곡은 Gemini 없이 초성 힌트를 달아 대기열에 넣는다.")
    void queuesPopularHangulSong() {
        given(melonChartClient.fetchChart()).willReturn(List.of(
                entry(1L, "밤양갱", "비비", POPULAR),
                entry(2L, "좋은날", "아이유", 49_999)));
        given(melonChartClient.fetchDetail(1L)).willReturn(BALLAD);

        collector.collect();

        assertThat(queued()).containsExactly(Map.of(
                "title", "밤양갱", "singer", "비비", "hint", "ㅂㅇㄱ", "release_date", RELEASE_DATE.toString()));
        verify(melonChartClient, never()).fetchDetail(2L);
        verify(geminiTitleReader, never()).read(any());
        assertThat(lastEvent()).isEqualTo(new MelonChartCollected(MelonChartCollected.Result.OK, 2, 1, 1, 0));
    }

    @Test
    @DisplayName("장르로 카테고리를 정한다.")
    void categorizesByGenre() {
        given(melonChartClient.fetchChart()).willReturn(List.of(entry(1L, "밤양갱", "비비", POPULAR)));
        given(melonChartClient.fetchDetail(1L)).willReturn(BALLAD);

        collector.collect();

        assertThat(jdbcTemplate.queryForObject("select categories from song_scrape_request", String.class))
                .contains(Category.KPOP.name(), Category.BALLAD.name());
    }

    @Test
    @DisplayName("이미 저장됐거나 대기열에 있는 곡은 상세 페이지도 보지 않는다.")
    void skipsKnownSongWithoutDetail() {
        songService.createSongQuiz(Song.of("밤양갱", "비비", List.of(Category.KPOP), RELEASE_DATE, null, 0,
                List.of(), "ㅂㅇㄱ"));
        given(melonChartClient.fetchChart()).willReturn(List.of(entry(1L, "밤양갱", "비비", POPULAR)));

        collector.collect();

        verify(melonChartClient, never()).fetchDetail(anyLong());
        assertThat(queued()).hasSize(1);
    }

    @Test
    @DisplayName("상세 페이지는 한 번에 30곡까지만 본다.")
    void limitsDetailsPerRun() {
        List<MelonChartEntry> chart = new ArrayList<>();
        for (int i = 0; i < 31; i++) {
            chart.add(entry(i, "노래" + (char) ('가' + i), "가수", POPULAR));
        }
        given(melonChartClient.fetchChart()).willReturn(chart);
        given(melonChartClient.fetchDetail(anyLong())).willReturn(BALLAD);

        collector.collect();

        verify(melonChartClient, times(30)).fetchDetail(anyLong());
        assertThat(queued()).hasSize(30);
    }

    @Test
    @DisplayName("한 곡의 상세 페이지가 실패해도 나머지 곡은 넣는다.")
    void continuesAfterDetailFailure() {
        given(melonChartClient.fetchChart()).willReturn(List.of(
                entry(1L, "밤양갱", "비비", POPULAR),
                entry(2L, "봄날", "방탄소년단", POPULAR)));
        given(melonChartClient.fetchDetail(1L)).willThrow(new MelonUnavailableException("시간 초과"));
        given(melonChartClient.fetchDetail(2L)).willReturn(BALLAD);

        collector.collect();

        assertThat(queued()).extracting(row -> row.get("title")).containsExactly("봄날");
    }

    @Test
    @DisplayName("한글이 아닌 제목은 한 번의 Gemini 호출로 모아 읽고, 독음의 초성을 힌트로 둔다.")
    void readsNonHangulTitlesInOneCall() {
        given(melonChartClient.fetchChart()).willReturn(List.of(
                entry(1L, "Whiplash", "aespa", POPULAR),
                entry(2L, "0+0", "한로로", POPULAR)));
        given(melonChartClient.fetchDetail(anyLong())).willReturn(BALLAD);
        given(geminiTitleReader.read(any())).willReturn(Optional.of(List.of(
                new GeminiReading("1", "위플래시", List.of("위플래쉬")),
                new GeminiReading("2", "영 더하기 영", List.of()))));

        collector.collect();

        ArgumentCaptor<List<GeminiTitle>> titles = titlesCaptor();
        verify(geminiTitleReader, times(1)).read(titles.capture());
        assertThat(titles.getValue()).containsExactlyInAnyOrder(
                new GeminiTitle("1", "Whiplash", "aespa"),
                new GeminiTitle("2", "0+0", "한로로"));
        assertThat(queued()).extracting(row -> row.get("title") + "=" + row.get("hint"))
                .containsExactlyInAnyOrder("Whiplash=ㅇㅍㄹㅅ", "0+0=ㅇㄷㅎㄱㅇ");
        assertThat(jdbcTemplate.queryForObject(
                "select answers from song_scrape_request where title = 'Whiplash'", String.class))
                .contains("whiplash", "위플래시", "위플래쉬");
    }

    @Test
    @DisplayName("Gemini 가 실패하면 한글이 아닌 곡만 다음 날로 미루고 한글 제목 곡은 넣는다.")
    void defersNonHangulSongsWhenGeminiFails() {
        given(melonChartClient.fetchChart()).willReturn(List.of(
                entry(1L, "Whiplash", "aespa", POPULAR),
                entry(2L, "밤양갱", "비비", POPULAR)));
        given(melonChartClient.fetchDetail(anyLong())).willReturn(BALLAD);
        given(geminiTitleReader.read(any())).willReturn(Optional.empty());

        collector.collect();

        assertThat(queued()).extracting(row -> row.get("title")).containsExactly("밤양갱");
        assertThat(lastEvent()).isEqualTo(
                new MelonChartCollected(MelonChartCollected.Result.GEMINI_FAILED, 2, 2, 1, 1));
    }

    @Test
    @DisplayName("독음이 한글이 아니거나 빠진 곡은 미룬다.")
    void defersSongWithUnusableReading() {
        given(melonChartClient.fetchChart()).willReturn(List.of(
                entry(1L, "Whiplash", "aespa", POPULAR),
                entry(2L, "Supernova", "aespa", POPULAR),
                entry(3L, "Drama", "aespa", POPULAR)));
        given(melonChartClient.fetchDetail(anyLong())).willReturn(BALLAD);
        given(geminiTitleReader.read(any())).willReturn(Optional.of(List.of(
                new GeminiReading("1", "Whiplash", List.of()),
                new GeminiReading("2", "슈퍼노바", List.of()))));

        collector.collect();

        assertThat(queued()).extracting(row -> row.get("title")).containsExactly("Supernova");
        assertThat(lastEvent().deferred()).isEqualTo(2);
    }

    @Test
    @DisplayName("차트에서 곡을 하나도 찾지 못하면 비었다고 알린다.")
    void reportsEmptyChart() {
        given(melonChartClient.fetchChart()).willReturn(List.of());

        collector.collect();

        assertThat(lastEvent().result()).isEqualTo(MelonChartCollected.Result.EMPTY);
    }

    @Test
    @DisplayName("멜론 요청이 실패해도 예외를 던지지 않고 실패를 알린다.")
    void reportsMelonFailure() {
        given(melonChartClient.fetchChart()).willThrow(new MelonUnavailableException("503"));

        assertThatCode(() -> collector.collect()).doesNotThrowAnyException();
        assertThat(lastEvent().result()).isEqualTo(MelonChartCollected.Result.FAILED);
    }

    @Test
    @DisplayName("넣은 곡은 기존 유튜브 링크 채우기를 거쳐 곡 목록까지 간다.")
    void queuedSongReachesSongTable() {
        given(melonChartClient.fetchChart()).willReturn(List.of(entry(1L, "밤양갱", "비비", POPULAR)));
        given(melonChartClient.fetchDetail(1L)).willReturn(BALLAD);
        given(youtubeScraper.findVideoId(anyString(), eq("비비"))).willReturn(Optional.of("BzYnNdJhZQw"));

        collector.collect();
        songScrapeService.fillPendingSongs();

        assertThat(jdbcTemplate.queryForList("select title, hint from song_entity"))
                .containsExactly(Map.of("title", "밤양갱", "hint", "ㅂㅇㄱ"));
        assertThat(queued()).isEmpty();
    }

    private static MelonChartEntry entry(long songId, String title, String singer, long likes) {
        return new MelonChartEntry(songId, title, List.of(singer), likes);
    }

    private List<Map<String, Object>> queued() {
        return jdbcTemplate.queryForList(
                "select title, singer, hint, cast(release_date as char) as release_date from song_scrape_request");
    }

    private MelonChartCollected lastEvent() {
        List<MelonChartCollected> events = eventCapture.getEvents(MelonChartCollected.class);
        assertThat(events).isNotEmpty();

        return events.getLast();
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<GeminiTitle>> titlesCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
