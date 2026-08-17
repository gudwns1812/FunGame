package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.ActionResult;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SongQuiz extends AbstractQuiz {

    private static final int ROUND_NOT_STARTED = -1;
    private static final String SINGER_TITLE_SEPARATOR = " - ";

    private final List<Song> songs;
    private final Category gameCategory;
    private final AtomicInteger currentIdx = new AtomicInteger(ROUND_NOT_STARTED);

    public SongQuiz(List<Song> songs, Category gameCategory) {
        this.songs = songs;
        this.gameCategory = gameCategory;
    }

    @Override
    public QuizContent getStatus() {
        int current = currentIdx.get();
        return QuizContent.of(songs.get(current).getLink());
    }

    @Override
    public QuizInfo getQuizInfo() {
        return new QuizInfo(getType().name(), gameCategory.name(), songs.size());
    }

    @Override
    public Long getCurrentContentId() {
        return currentSong()
                .map(Song::getId)
                .orElse(null);
    }

    private Optional<Song> currentSong() {
        int current = currentIdx.get();
        if (current < 0 || current >= songs.size()) {
            return Optional.empty();
        }

        return Optional.of(songs.get(current));
    }

    @Override
    public GameType getType() {
        return GameType.SONG;
    }

    @Override
    public ActionResult submitAnswer(Long memberId, String answer) {
        if (!isRoundStarted()) {
            return ActionResult.NO_ACTION;
        }

        Song song = songs.get(currentIdx.get());
        return song.isCorrect(answer) ? ActionResult.CORRECT : ActionResult.WRONG;
    }

    @Override
    public QuizAnswer getAnswer() {
        int current = currentIdx.get();
        Song song = songs.get(current);

        return new QuizAnswer(
                String.join(" ", song.getSinger(), SINGER_TITLE_SEPARATOR, song.getTitle()),
                String.join(" ", SINGER_TITLE_SEPARATOR, song.getTitle()));
    }

    @Override
    public void startRound() {
        currentIdx.incrementAndGet();
    }

    @Override
    public boolean isRoundStarted() {
        return currentIdx.get() != ROUND_NOT_STARTED;
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
