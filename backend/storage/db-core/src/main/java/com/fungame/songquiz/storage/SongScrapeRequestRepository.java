package com.fungame.songquiz.storage;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SongScrapeRequestRepository extends JpaRepository<SongScrapeRequestEntity, Long> {

    List<SongScrapeRequestEntity> findAllByOrderByCreatedAtAsc(Limit limit);

    boolean existsBySingerAndTitle(String singer, String title);

    boolean existsByTitleAndReleaseDate(String title, LocalDate releaseDate);
}
