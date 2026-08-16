package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.storage.SongScrapeRequestEntity;
import com.fungame.songquiz.storage.SongScrapeRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    public boolean existsBySingerAndTitle(String singer, String title) {
        return songScrapeRequestRepository.existsBySingerAndTitle(singer, title);
    }

    @Transactional(readOnly = true)
    public boolean existsByTitleAndReleaseDate(String title, LocalDate releaseDate) {
        return songScrapeRequestRepository.existsByTitleAndReleaseDate(title, releaseDate);
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
