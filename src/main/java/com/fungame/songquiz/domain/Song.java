package com.fungame.songquiz.domain;

import java.time.LocalDate;
import java.util.ArrayList;
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

    public boolean isCorrect(String answer) {
        return answers.stream()
                .anyMatch(a -> a.equalsIgnoreCase(answer.trim()));
    }

    public static Song of(String title, String singer, Category category, LocalDate releaseDate, String link,
                          int playSeconds, List<String> answers) {
        var answersWithTitle = new ArrayList<>(answers);
        answersWithTitle.add(title);
        
        return new Song(title, singer, category, releaseDate, link, playSeconds, answersWithTitle);
    }
}
