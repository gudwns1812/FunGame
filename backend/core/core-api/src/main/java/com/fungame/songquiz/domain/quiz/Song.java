package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.Category;
import lombok.Getter;
import lombok.With;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class Song {
    private final Long id;
    private final String title;
    private final String singer;
    private final List<Category> categories;
    private final LocalDate releaseDate;
    @With
    private final String link;
    private final int playSeconds;
    private final Set<String> answers;
    private final String hint;

    private Song(Long id, String title, String singer, List<Category> categories, LocalDate releaseDate, String link,
                 int playSeconds,
                 Set<String> answers, String hint) {
        this.id = id;
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
        return answers.contains(answer);
    }

    public static Song of(String title, String singer, List<Category> categories, LocalDate releaseDate, String link,
                          int playSeconds, List<String> answers, String hint) {
        return new Song(null, title, singer, categories, releaseDate, link, playSeconds,
                answersWithTitle(answers, title), hint);
    }

    public static Song stored(Long id, String title, String singer, List<Category> categories, LocalDate releaseDate,
                              String link, int playSeconds, List<String> answers, String hint) {
        return new Song(id, title, singer, categories, releaseDate, link, playSeconds,
                answersWithTitle(answers, title), hint);
    }

    private static Set<String> answersWithTitle(List<String> answers, String title) {
        Set<String> withTitle = new HashSet<>(answers);
        withTitle.add(title);

        return withTitle;
    }
}
