package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameAnswerDto;
import com.fungame.songquiz.domain.dto.GameContentDto;
import com.fungame.songquiz.domain.dto.GameInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SongQuiz extends AbstractQuizGame {

    private static final int ROUND_NOT_STARTED = -1;

    private final List<Song> songs;
    private final Category gameCategory;
    private final AtomicInteger currentIdx = new AtomicInteger(ROUND_NOT_STARTED);

    public SongQuiz(List<Song> songs, Category gameCategory) {
        super(null);
        this.songs = songs;
        this.gameCategory = gameCategory;
    }

    @Override
    public GameContentDto getStatus() {
        int current = currentIdx.get();
        return GameContentDto.from(this, songs.get(current).getLink());
    }

    @Override
    public GameInfo getGameInfo() {
        return new GameInfo(getType().name(), gameCategory.name(), songs.size());
    }

    @Override
    public GameType getType() {
        return GameType.SONG;
    }

    @Override
    protected ActionResult processAnswer(String playerName, String answer) {
        int current = currentIdx.get();
        if (current == ROUND_NOT_STARTED) {
            return ActionResult.NO_ACTION;
        }

        Song song = songs.get(current);
        return song.isCorrect(answer) ? ActionResult.CORRECT : ActionResult.WRONG;
    }

    @Override
    public GameAnswerDto getAnswer() {
        int current = currentIdx.get();
        Song song = songs.get(current);

        return GameAnswerDto.from(this, song.getSinger(), " - ", song.getTitle());
    }

    @Override
    public void startRound() {
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

    @Override
    public String getHint() {
        int songIdx = currentIdx.get();

        Song song = songs.get(songIdx);
        return song.getSinger() + " - " + song.getHint();
    }
}
