package com.fungame.songquiz.client.melon;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Component
public class MelonChartClient {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final int TIMEOUT_MILLIS = 5000;
    private static final Duration REQUEST_INTERVAL = Duration.ofSeconds(1);

    private long lastRequestNanos;

    public List<MelonChartEntry> fetchChart() {
        List<MelonPages.ChartRow> rows = MelonPages.chartRows(get(MelonPages.CHART_URL));
        if (rows.isEmpty()) {
            return List.of();
        }

        String likesJson = get(MelonPages.likesUrl(rows.stream().map(MelonPages.ChartRow::songId).toList()));
        return MelonPages.withLikes(rows, likesJson);
    }

    public MelonSongDetail fetchDetail(long songId) {
        return MelonPages.detail(get(MelonPages.DETAIL_URL + songId));
    }

    private synchronized String get(String url) {
        waitForInterval();
        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MILLIS)
                    .ignoreContentType(true)
                    .execute()
                    .body();
        } catch (IOException e) {
            throw new MelonUnavailableException("멜론 요청에 실패했다: " + url, e);
        } finally {
            lastRequestNanos = System.nanoTime();
        }
    }

    private void waitForInterval() {
        if (lastRequestNanos == 0) {
            return;
        }

        long remainingNanos = REQUEST_INTERVAL.toNanos() - (System.nanoTime() - lastRequestNanos);
        if (remainingNanos <= 0) {
            return;
        }

        try {
            Thread.sleep(Duration.ofNanos(remainingNanos));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MelonUnavailableException("멜론 요청 간격을 기다리다 중단됐다", e);
        }
    }
}
