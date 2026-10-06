package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.Category;
import java.time.LocalDate;
import java.util.List;

public record SongSnapshot(
        Long id,
        String title,
        String singer,
        List<Category> categories,
        LocalDate releaseDate,
        String link,
        int playSeconds,
        List<String> answers,
        String hint
) {

    static SongSnapshot from(Song song) {
        return new SongSnapshot(song.getId(), song.getTitle(), song.getSinger(), song.getCategories(),
                song.getReleaseDate(), song.getLink(), song.getPlaySeconds(), List.copyOf(song.getAnswers()),
                song.getHint());
    }

    Song toSong() {
        return Song.stored(id, title, singer, categories, releaseDate, link, playSeconds, answers, hint);
    }
}
