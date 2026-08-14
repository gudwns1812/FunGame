package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.SongEntity;
import com.fungame.songquiz.storage.SongStore;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import com.fungame.songquiz.support.extern.YoutubeScraper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class SongService {

    private final YoutubeScraper scraper;
    private final SongStore songStore;

    public List<Long> getRandomSongIds(int count) {
        List<Long> allIds = songStore.findAll().stream()
                .map(SongEntity::getId)
                .collect(Collectors.toList());

        Collections.shuffle(allIds);

        return allIds.stream()
                .limit(count)
                .collect(Collectors.toList());
    }


    public void createSongQuiz(Song song) {
        String videoLink = scraper.getVideoId(song.getTitle(), song.getSinger());

        boolean exists = songStore.existsBySingerAndTitle(song.getSinger(), song.getTitle());
        if (exists) {
            throw new CoreException(ErrorType.QUIZ_DUPLICATE_ERROR);
        }

        SongEntity newSong = SongEntity.builder()
                .title(song.getTitle())
                .singer(song.getSinger())
                .categories(song.getCategories())
                .playSeconds(song.getPlaySeconds())
                .answers(new ArrayList<>(song.getAnswers()))
                .videoLink(videoLink)
                .releaseDate(song.getReleaseDate())
                .hint(song.getHint())
                .build();

        songStore.save(newSong);
    }

    public boolean existSongQuiz(String title, LocalDate releaseDate) {
        if (releaseDate == null) {
            return songStore.existsByTitleContaining(title);
        }
        return songStore.existsByTitleContainingAndReleaseDate(title, releaseDate);
    }
}
