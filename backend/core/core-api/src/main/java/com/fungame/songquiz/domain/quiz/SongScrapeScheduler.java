package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.support.config.ClusterWide;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.song-scrape.enabled", matchIfMissing = true)
public class SongScrapeScheduler {

    private final SongScrapeService songScrapeService;

    @ClusterWide
    @Scheduled(fixedDelayString = "${app.song-scrape.interval-millis:60000}")
    public void fillPendingSongs() {
        songScrapeService.fillPendingSongs();
    }
}
