package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.client.gemini.GeminiReading;
import com.fungame.songquiz.client.gemini.GeminiTitle;
import com.fungame.songquiz.client.gemini.GeminiTitleReader;
import com.fungame.songquiz.client.melon.MelonChartClient;
import com.fungame.songquiz.client.melon.MelonChartEntry;
import com.fungame.songquiz.client.melon.MelonSongDetail;
import com.fungame.songquiz.domain.quiz.MelonChartCollected.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MelonChartCollector {

    private static final int MAX_DETAILS_PER_RUN = 30;

    private final MelonChartClient melonChartClient;
    private final GeminiTitleReader geminiTitleReader;
    private final SongService songService;
    private final ApplicationEventPublisher eventPublisher;

    private record Found(ChartSong chartSong, MelonSongDetail detail) {

        Song toSong() {
            return chartSong.toSong(detail.releaseDate(), MelonGenre.categoriesOf(detail.genres()));
        }

        Song toSong(TitleReading reading) {
            return chartSong.toSong(detail.releaseDate(), MelonGenre.categoriesOf(detail.genres()), reading);
        }

        String id() {
            return String.valueOf(chartSong.melonId());
        }

        String label() {
            return chartSong.singer() + " - " + chartSong.title().name();
        }
    }

    public void collect() {
        MelonChartCollected collected;
        try {
            collected = collectChart();
        } catch (RuntimeException e) {
            log.error("멜론 차트 수집에 실패했다", e);
            collected = MelonChartCollected.failed();
        }

        log.info("멜론 차트 수집: 결과 {} · 차트 {} · 5만 이상 {} · 넣음 {} · 미룸 {}",
                collected.result(), collected.charted(), collected.popular(), collected.enqueued(),
                collected.deferred());
        eventPublisher.publishEvent(collected);
    }

    private MelonChartCollected collectChart() {
        List<MelonChartEntry> chart = melonChartClient.fetchChart();
        if (chart.isEmpty()) {
            log.error("멜론 차트에서 곡을 하나도 찾지 못했다. 페이지 구조가 바뀌었을 수 있다");
            return MelonChartCollected.empty();
        }

        List<ChartSong> popular = chart.stream()
                .map(entry -> ChartSong.of(entry.songId(), entry.title(), entry.singers(), entry.likes()))
                .filter(ChartSong::isPopular)
                .toList();

        List<Found> found = popular.stream()
                .filter(song -> !songService.isKnown(song.singer(), song.title().name()))
                .limit(MAX_DETAILS_PER_RUN)
                .map(this::withDetail)
                .flatMap(Optional::stream)
                .toList();

        List<Found> hangulTitles = found.stream().filter(song -> !song.chartSong().needsReading()).toList();
        List<Found> needReading = found.stream().filter(song -> song.chartSong().needsReading()).toList();

        long enqueued = hangulTitles.stream().filter(song -> enqueue(song, song.toSong())).count();

        Optional<Map<String, TitleReading>> readings = readTitles(needReading);
        if (readings.isEmpty()) {
            log.warn("Gemini 로 제목을 읽지 못해 {}곡을 다음 날로 미룬다", needReading.size());
            return new MelonChartCollected(Result.GEMINI_FAILED, chart.size(), popular.size(), (int) enqueued,
                    needReading.size());
        }

        List<Found> readable = needReading.stream().filter(song -> isReadable(song, readings.get())).toList();
        enqueued += readable.stream()
                .filter(song -> enqueue(song, song.toSong(readings.get().get(song.id()))))
                .count();

        return new MelonChartCollected(Result.OK, chart.size(), popular.size(), (int) enqueued,
                needReading.size() - readable.size());
    }

    private Optional<Found> withDetail(ChartSong chartSong) {
        try {
            return Optional.of(new Found(chartSong, melonChartClient.fetchDetail(chartSong.melonId())));
        } catch (RuntimeException e) {
            log.warn("멜론 상세 페이지를 읽지 못해 건너뛴다: {} - {}", chartSong.singer(), chartSong.title().name(), e);
            return Optional.empty();
        }
    }

    private Optional<Map<String, TitleReading>> readTitles(List<Found> needReading) {
        if (needReading.isEmpty()) {
            return Optional.of(Map.of());
        }

        List<GeminiTitle> titles = needReading.stream()
                .map(song -> new GeminiTitle(song.id(), song.chartSong().title().name(), song.chartSong().singer()))
                .toList();

        return geminiTitleReader.read(titles).map(MelonChartCollector::byId);
    }

    private static Map<String, TitleReading> byId(List<GeminiReading> readings) {
        return readings.stream()
                .filter(reading -> reading.id() != null)
                .collect(Collectors.toMap(GeminiReading::id,
                        reading -> new TitleReading(reading.pronunciation(), reading.aliases()),
                        (first, second) -> first));
    }

    private static boolean isReadable(Found song, Map<String, TitleReading> readings) {
        TitleReading reading = readings.get(song.id());
        if (reading != null && reading.isUsable()) {
            return true;
        }

        log.warn("쓸 수 있는 독음이 없어 다음 날로 미룬다: {} ({})", song.label(),
                reading == null ? "응답에 없음" : reading.pronunciation());
        return false;
    }

    private boolean enqueue(Found found, Song song) {
        try {
            return songService.enqueueIfNew(song);
        } catch (RuntimeException e) {
            log.warn("곡을 대기열에 넣지 못했다: {}", found.label(), e);
            return false;
        }
    }
}
