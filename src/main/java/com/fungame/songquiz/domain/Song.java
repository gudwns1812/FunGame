package com.fungame.songquiz.domain;

import java.time.LocalDate;
import java.util.List;

public class Song {
    private final String title;
    private final String singer;
    private final Category category;
    private final LocalDate releaseDate;
    private final String link;
    private final int playSeconds;
    private final List<String> answers;

    private Song(String title, String singer, Category category, LocalDate releaseDate, String link, int playSeconds,
                 List<String> answers) {
        this.title = title;
        this.singer = singer;
        this.category = category;
        this.releaseDate = releaseDate;
        this.link = link;
        this.playSeconds = playSeconds;
        this.answers = answers;
    }

    public boolean hasTitle(String title) {
        return this.title.equals(title);
    }

    public static Song of(String title, String singer, Category category, LocalDate releaseDate, String link,
                          int playSeconds,
                          List<String> answers) {
        return new Song(title, singer, category, releaseDate, link, playSeconds, answers);
    }
}
