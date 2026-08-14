package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.SongScrapeRequestEntity;
import com.fungame.songquiz.storage.SongScrapeRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class SongScrapeRequestWriter {

    private final SongScrapeRequestRepository songScrapeRequestRepository;

    @Transactional
    public void append(SongScrapeRequest request) {
        songScrapeRequestRepository.save(SongScrapeRequestEntity.builder()
                .title(request.getTitle())
                .singer(request.getSinger())
                .categories(request.getCategories())
                .releaseDate(request.getReleaseDate())
                .playSeconds(request.getPlaySeconds())
                .answers(new ArrayList<>(request.getAnswers()))
                .hint(request.getHint())
                .build());
    }

    @Transactional
    public void remove(SongScrapeRequest request) {
        songScrapeRequestRepository.deleteById(request.getId());
    }
}
