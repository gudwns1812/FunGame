package com.fungame.songquiz.controller.request;

import com.fungame.songquiz.domain.quiz.Song;
import com.fungame.songquiz.enums.Category;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateSongQuizRequest {
    private String singer;
    private String title;
    private List<Category> categories;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;
    private List<String> answers;
    private String hint;

    public Song toSong() {
        return Song.of(title, singer, categories, releaseDate, null, 0, answers, hint);
    }
}
