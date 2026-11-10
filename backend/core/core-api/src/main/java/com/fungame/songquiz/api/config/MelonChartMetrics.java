package com.fungame.songquiz.api.config;

import com.fungame.songquiz.domain.quiz.MelonChartCollected;
import com.fungame.songquiz.domain.quiz.MelonChartCollected.Result;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

@Component
public class MelonChartMetrics {

    private static final String RUNS_METER = "fungame.melon.chart.runs";
    private static final String SONGS_METER = "fungame.melon.chart.songs";

    private final Map<Result, Counter> runs = new EnumMap<>(Result.class);
    private final Counter popular;
    private final Counter enqueued;
    private final Counter deferred;

    public MelonChartMetrics(MeterRegistry meterRegistry) {
        Arrays.stream(Result.values()).forEach(result -> runs.put(result, Counter.builder(RUNS_METER)
                .description("멜론 차트 수집 횟수. ok 가 아닌 결과는 알림 대상이다")
                .tag("result", result.name().toLowerCase(Locale.ROOT))
                .register(meterRegistry)));
        popular = songs(meterRegistry, "popular");
        enqueued = songs(meterRegistry, "enqueued");
        deferred = songs(meterRegistry, "deferred");
    }

    @EventListener
    public void record(MelonChartCollected collected) {
        runs.get(collected.result()).increment();
        popular.increment(collected.popular());
        enqueued.increment(collected.enqueued());
        deferred.increment(collected.deferred());
    }

    private static Counter songs(MeterRegistry meterRegistry, String stage) {
        return Counter.builder(SONGS_METER)
                .description("멜론 차트 수집에서 단계별 곡 수. popular 5만 이상 · enqueued 대기열에 넣음 · deferred 독음이 없어 미룸")
                .tag("stage", stage)
                .register(meterRegistry);
    }
}
