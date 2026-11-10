package com.fungame.songquiz.client.melon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MelonPagesTest {

    @Test
    @DisplayName("차트 행마다 곡 id · 제목 · 가수를 읽고 좋아요 수를 붙인다. NBSP 는 공백으로 읽힌다.")
    void readsChartWithLikes() {
        List<MelonChartEntry> entries = MelonPages.chartEntries(fixture("chart.html"), fixture("likes.json"));

        assertThat(entries).containsExactly(
                new MelonChartEntry(603712834L, "With", List.of("RESCENE (리센느)"), 66_987),
                new MelonChartEntry(603382333L, "NEON", List.of("A2O", "A2O LITE"), 66_733),
                new MelonChartEntry(38242510L, "HOME SWEET HOME (feat. 태양, 대성)", List.of("G-DRAGON"), 234_781));
    }

    @Test
    @DisplayName("좋아요 응답에 없는 곡은 좋아요 0 으로 둔다.")
    void missingLikesAreZero() {
        List<MelonChartEntry> entries = MelonPages.chartEntries(fixture("chart.html"), "{\"contsLike\":[]}");

        assertThat(entries).extracting(MelonChartEntry::likes).containsOnly(0L);
    }

    @Test
    @DisplayName("차트 행을 하나도 찾지 못하면 빈 목록이다.")
    void emptyWhenNoRows() {
        assertThat(MelonPages.chartRows("<html><body><p>점검 중</p></body></html>")).isEmpty();
    }

    @Test
    @DisplayName("좋아요 조회 주소에 곡 id 를 쉼표로 잇는다.")
    void likesUrlJoinsSongIds() {
        assertThat(MelonPages.likesUrl(List.of(603712834L, 603382333L)))
                .isEqualTo("https://www.melon.com/commonlike/getSongLike.json?contsIds=603712834,603382333");
    }

    @Test
    @DisplayName("상세 페이지에서 발매일과 장르를 읽는다.")
    void readsDetail() {
        assertThat(MelonPages.detail(fixture("detail-34061322.html")))
                .isEqualTo(new MelonSongDetail(LocalDate.of(2021, 10, 11), List.of("발라드", "국내드라마")));
        assertThat(MelonPages.detail(fixture("detail-37901626.html")))
                .isEqualTo(new MelonSongDetail(LocalDate.of(2024, 8, 16), List.of("POP")));
    }

    @Test
    @DisplayName("상세 페이지에 발매일이 없으면 실패한다.")
    void failsWithoutReleaseDate() {
        assertThatThrownBy(() -> MelonPages.detail("<html><body><div class=\"meta\"></div></body></html>"))
                .isInstanceOf(MelonUnavailableException.class);
    }

    @Test
    @DisplayName("발매일이 날짜 모양이 아니면 실패한다.")
    void failsWithMalformedReleaseDate() {
        String html = "<div class=\"meta\"><dl class=\"list\"><dt>발매일</dt><dd>2021.10</dd>"
                + "<dt>장르</dt><dd>발라드</dd></dl></div>";

        assertThatThrownBy(() -> MelonPages.detail(html)).isInstanceOf(MelonUnavailableException.class);
    }

    private static String fixture(String name) {
        try (InputStream in = MelonPagesTest.class.getResourceAsStream("/melon/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
