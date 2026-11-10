package com.fungame.songquiz.api.config;

import com.fungame.songquiz.domain.quiz.MelonChartCollected;
import com.fungame.songquiz.domain.quiz.MelonChartCollected.Result;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MelonChartMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MelonChartMetrics metrics = new MelonChartMetrics(registry);

    @Test
    @DisplayName("기동하자마자 모든 결과와 단계의 카운터가 0 으로 있다. 처음 생긴 카운터의 첫 값은 increase() 가 못 잡는다.")
    void registersAllCountersAtZero() {
        for (String result : new String[]{"ok", "empty", "failed", "gemini_failed"}) {
            assertThat(registry.get("fungame.melon.chart.runs").tag("result", result).counter().count()).isZero();
        }
        for (String stage : new String[]{"popular", "enqueued", "deferred"}) {
            assertThat(registry.get("fungame.melon.chart.songs").tag("stage", stage).counter().count()).isZero();
        }
    }

    @Test
    @DisplayName("수집 결과마다 결과 카운터를 하나 올리고 곡 수를 단계별로 더한다.")
    void recordsCollection() {
        metrics.record(new MelonChartCollected(Result.GEMINI_FAILED, 100, 71, 25, 5));

        assertThat(registry.get("fungame.melon.chart.runs").tag("result", "gemini_failed").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("fungame.melon.chart.runs").tag("result", "ok").counter().count()).isZero();
        assertThat(registry.get("fungame.melon.chart.songs").tag("stage", "popular").counter().count())
                .isEqualTo(71);
        assertThat(registry.get("fungame.melon.chart.songs").tag("stage", "enqueued").counter().count())
                .isEqualTo(25);
        assertThat(registry.get("fungame.melon.chart.songs").tag("stage", "deferred").counter().count())
                .isEqualTo(5);
    }
}
