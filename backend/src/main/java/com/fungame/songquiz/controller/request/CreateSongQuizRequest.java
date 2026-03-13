package com.fungame.songquiz.controller.request;

import com.fungame.songquiz.domain.Category;
import com.fungame.songquiz.domain.Song;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateSongQuizRequest {
    private String singer;
    private String title;
    private List<Category> categories;
    private LocalDate releaseDate;
    private List<String> answers;
    private String hint;

    public Song toSong() {
        return Song.of(title, singer, categories, releaseDate, null, 0, answers, hint);
    }
}
