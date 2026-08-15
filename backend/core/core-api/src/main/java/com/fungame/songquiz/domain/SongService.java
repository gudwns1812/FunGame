package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.SongEntity;
import com.fungame.songquiz.storage.SongRepository;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class SongService {

    private final SongRepository songRepository;
    private final SongScrapeRequestReader songScrapeRequestReader;
    private final SongScrapeRequestWriter songScrapeRequestWriter;

    public List<Long> getRandomSongIds(int count) {
        List<Long> allIds = songRepository.findAll().stream()
                .map(SongEntity::getId)
                .collect(Collectors.toList());

        Collections.shuffle(allIds);

        return allIds.stream()
                .limit(count)
                .collect(Collectors.toList());
    }


    @Transactional
    public void createSongQuiz(Song song) {
        if (isDuplicate(song)) {
            throw new CoreException(ErrorType.QUIZ_DUPLICATE_ERROR);
        }

        songScrapeRequestWriter.append(SongScrapeRequest.of(song));
    }

    private boolean isDuplicate(Song song) {
        return songRepository.existsBySingerAndTitle(song.getSinger(), song.getTitle())
                || songRepository.existsByTitleAndReleaseDate(song.getTitle(), song.getReleaseDate())
                || songScrapeRequestReader.existsBySingerAndTitle(song.getSinger(), song.getTitle())
                || songScrapeRequestReader.existsByTitleAndReleaseDate(song.getTitle(), song.getReleaseDate());
    }

    public boolean existSongQuiz(String title, LocalDate releaseDate) {
        if (releaseDate == null) {
            return songRepository.existsByTitleContaining(title);
        }
        return songRepository.existsByTitleContainingAndReleaseDate(title, releaseDate);
    }
}
