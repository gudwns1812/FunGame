package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.storage.SongScrapeRequestEntity;
import com.fungame.songquiz.storage.SongScrapeRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SongScrapeRequestReader {

    private final SongScrapeRequestRepository songScrapeRequestRepository;

    @Transactional(readOnly = true)
    public List<SongScrapeRequest> findOldest(int count) {
        return songScrapeRequestRepository.findAllByOrderByCreatedAtAsc(Limit.of(count)).stream()
                .map(SongScrapeRequestReader::toRequest)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean existsSameSong(Song song) {
        return songScrapeRequestRepository.existsBySingerAndTitle(song.getSinger(), song.getTitle())
                || songScrapeRequestRepository.existsByTitleAndReleaseDate(song.getTitle(), song.getReleaseDate());
    }

    private static SongScrapeRequest toRequest(SongScrapeRequestEntity entity) {
        return SongScrapeRequest.restore(
                entity.getId(),
                entity.getTitle(),
                entity.getSinger(),
                entity.getCategories(),
                entity.getReleaseDate(),
                entity.getPlaySeconds(),
                entity.getAnswers(),
                entity.getHint());
    }
}
