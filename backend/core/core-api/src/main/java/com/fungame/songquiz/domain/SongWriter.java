package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.SongEntity;
import com.fungame.songquiz.storage.SongUpsertDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class SongWriter {

    private final SongUpsertDao songUpsertDao;

    @Transactional
    public void upsertByVideoLink(Song song) {
        songUpsertDao.upsertByVideoLink(toQuiz(song), song.getLink());
    }

    private static SongEntity.Quiz toQuiz(Song song) {
        return new SongEntity.Quiz(
                song.getTitle(),
                song.getSinger(),
                song.getCategories(),
                song.getReleaseDate(),
                song.getPlaySeconds(),
                new ArrayList<>(song.getAnswers()),
                song.getHint());
    }
}
