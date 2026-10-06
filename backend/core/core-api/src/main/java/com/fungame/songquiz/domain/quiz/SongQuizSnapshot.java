package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.Category;
import java.util.List;

public record SongQuizSnapshot(
        List<SongSnapshot> songs,
        Category category,
        int currentIdx,
        boolean roundProcessing
) implements QuizSnapshot {

    @Override
    public Quiz restore() {
        return SongQuiz.restore(this);
    }
}
