package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;


@Service
@RequiredArgsConstructor
public class SongService {

    private final SongReader songReader;
    private final SongScrapeRequestReader songScrapeRequestReader;
    private final SongScrapeRequestWriter songScrapeRequestWriter;

    public void createSongQuiz(Song song) {
        if (isDuplicate(song)) {
            throw new CoreException(ErrorType.QUIZ_DUPLICATE_ERROR);
        }

        songScrapeRequestWriter.append(SongScrapeRequest.of(song));
    }

    public boolean existSongQuiz(String title, LocalDate releaseDate) {
        return songReader.existsByTitleLike(title, releaseDate);
    }

    private boolean isDuplicate(Song song) {
        return songReader.existsSameSong(song) || songScrapeRequestReader.existsSameSong(song);
    }
}
