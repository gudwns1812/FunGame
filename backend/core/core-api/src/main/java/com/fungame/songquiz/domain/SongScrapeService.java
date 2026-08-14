package com.fungame.songquiz.domain;

import com.fungame.songquiz.support.extern.YoutubeScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongScrapeService {

    private static final int BATCH_SIZE = 10;

    private final SongScrapeRequestReader songScrapeRequestReader;
    private final SongScrapeRequestWriter songScrapeRequestWriter;
    private final SongWriter songWriter;
    private final YoutubeScraper youtubeScraper;

    public void fillPendingSongs() {
        songScrapeRequestReader.findOldest(BATCH_SIZE).forEach(this::fill);
    }

    private void fill(SongScrapeRequest request) {
        youtubeScraper.findVideoId(request.getTitle(), request.getSinger())
                .ifPresent(videoId -> {
                    songWriter.upsertByVideoLink(request.toSongWith(videoId));
                    songScrapeRequestWriter.remove(request);
                    log.info("대기 중이던 곡을 저장했다: {} - {}", request.getSinger(), request.getTitle());
                });
    }
}
