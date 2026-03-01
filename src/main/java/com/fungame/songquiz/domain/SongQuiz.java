package com.fungame.songquiz.domain;

import java.util.List;

public class SongQuiz implements Game {

    private final List<Song> songs;

    public SongQuiz(List<Song> songs) {
        this.songs = songs;
    }

    @Override
    public boolean isCorrect(String answer) {
        return songs.stream()
                .anyMatch(song -> song.isCorrect(answer));
    }
}
