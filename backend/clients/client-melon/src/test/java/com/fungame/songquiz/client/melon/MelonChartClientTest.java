package com.fungame.songquiz.client.melon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("external")
class MelonChartClientTest {

    MelonChartClient melonChartClient = new MelonChartClient();

    @Test
    @DisplayName("실제 멜론 차트에서 100곡과 좋아요 수를 가져온다.")
    void fetchesLiveChart() {
        List<MelonChartEntry> entries = melonChartClient.fetchChart();

        assertThat(entries).hasSize(100);
        assertThat(entries).allSatisfy(entry -> {
            assertThat(entry.title()).isNotBlank();
            assertThat(entry.singers()).isNotEmpty();
        });
        assertThat(entries).anySatisfy(entry -> assertThat(entry.likes()).isPositive());
    }

    @Test
    @DisplayName("실제 상세 페이지에서 발매일과 장르를 가져온다.")
    void fetchesLiveDetail() {
        MelonSongDetail detail = melonChartClient.fetchDetail(34061322L);

        assertThat(detail.releaseDate()).isEqualTo(LocalDate.of(2021, 10, 11));
        assertThat(detail.genres()).contains("발라드");
    }
}
