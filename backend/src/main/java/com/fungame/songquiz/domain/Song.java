package com.fungame.songquiz.domain;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.With;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class Song {
    private final String title;
    private final String singer;
    private final List<Category> categories;
    private final LocalDate releaseDate;
    @With
    private final String link;
    private final int playSeconds;
    private final Set<String> answers;
    private final String hint;

    private Song(String title, String singer, List<Category> categories, LocalDate releaseDate, String link,
                 int playSeconds,
                 Set<String> answers, String hint) {
        this.title = title;
        this.singer = singer;
        this.categories = categories;
        this.releaseDate = releaseDate;
        this.link = link;
        this.playSeconds = playSeconds;
        this.answers = answers;
        this.hint = hint;
    }

    public boolean isCorrect(String answer) {
        log.info("answer : {} , answers : {}", answer, answers);
        return answers.contains(answer);
    }

    public static Song of(String title, String singer, List<Category> categories, LocalDate releaseDate, String link,
                          int playSeconds, List<String> answers, String hint) {
        Set<String> answer = new HashSet<>(answers);
        answer.add(title);

        return new Song(title, singer, categories, releaseDate, link, playSeconds, answer, hint);
    }
}
