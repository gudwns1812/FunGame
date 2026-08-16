package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.Category;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class SongScrapeRequest {

    private final Long id;
    private final String title;
    private final String singer;
    private final List<Category> categories;
    private final LocalDate releaseDate;
    private final int playSeconds;
    private final List<String> answers;
    private final String hint;

    private SongScrapeRequest(Long id, String title, String singer, List<Category> categories, LocalDate releaseDate,
                              int playSeconds, List<String> answers, String hint) {
        this.id = id;
        this.title = title;
        this.singer = singer;
        this.categories = categories;
        this.releaseDate = releaseDate;
        this.playSeconds = playSeconds;
        this.answers = answers;
        this.hint = hint;
    }

    public static SongScrapeRequest of(Song song) {
        return new SongScrapeRequest(
                null,
                song.getTitle(),
                song.getSinger(),
                song.getCategories(),
                song.getReleaseDate(),
                song.getPlaySeconds(),
                List.copyOf(song.getAnswers()),
                song.getHint());
    }

    public static SongScrapeRequest restore(Long id, String title, String singer, List<Category> categories,
                                            LocalDate releaseDate, int playSeconds, List<String> answers,
                                            String hint) {
        return new SongScrapeRequest(id, title, singer, categories, releaseDate, playSeconds, answers, hint);
    }

    public Song toSongWith(String videoLink) {
        return Song.of(title, singer, categories, releaseDate, videoLink, playSeconds, answers, hint);
    }
}
