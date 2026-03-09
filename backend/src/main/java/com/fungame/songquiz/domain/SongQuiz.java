package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameAnswerDto;
import com.fungame.songquiz.domain.dto.GameContentDto;
import com.fungame.songquiz.domain.dto.GameInfo;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SongQuiz implements Game {

    private final List<Song> songs;
    private final Category gameCategory;
    private final AtomicInteger currentIdx = new AtomicInteger(0);

    public SongQuiz(List<Song> songs, Category gameCategory) {
        this.songs = songs;
        this.gameCategory = gameCategory;
    }

    @Override
    public GameContentDto getContent() {
        int current = currentIdx.get();
        return GameContentDto.from(this, songs.get(current).getLink());
    }

    @Override
    public GameInfo getGameInfo() {
        return new GameInfo("노래 맞추기", gameCategory.name(), songs.size());
    }

    @Override
    public boolean isCorrect(String answer) {
        Song song = songs.get(currentIdx.get());
        return song.isCorrect(answer);
    }

    @Override
    public GameAnswerDto getAnswer() {
        int current = currentIdx.get();
        Song song = songs.get(current);

        return GameAnswerDto.from(this, song.getSinger(), " - ", song.getTitle());
    }

    @Override
    public void nextRound() {
        currentIdx.incrementAndGet();
    }

    @Override
    public boolean isLast() {
        return currentIdx.get() >= songs.size() - 1;
    }

    @Override
    public int getCurrentRound() {
        return currentIdx.get() + 1;
    }

    @Override
    public int getTotalRound() {
        return songs.size();
    }
}
