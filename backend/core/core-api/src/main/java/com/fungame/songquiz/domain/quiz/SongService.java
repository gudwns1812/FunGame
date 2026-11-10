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
        if (!enqueueIfNew(song)) {
            throw new CoreException(ErrorType.QUIZ_DUPLICATE_ERROR);
        }
    }

    public boolean enqueueIfNew(Song song) {
        if (isDuplicate(song)) {
            return false;
        }

        songScrapeRequestWriter.append(SongScrapeRequest.of(song));
        return true;
    }

    public boolean isKnown(String singer, String title) {
        return songReader.existsBySingerAndTitle(singer, title)
                || songScrapeRequestReader.existsBySingerAndTitle(singer, title);
    }

    public boolean existSongQuiz(String title, LocalDate releaseDate) {
        return songReader.existsByTitleLike(title, releaseDate);
    }

    private boolean isDuplicate(Song song) {
        return songReader.existsSameSong(song) || songScrapeRequestReader.existsSameSong(song);
    }
}
