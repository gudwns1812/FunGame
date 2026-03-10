package com.fungame.songquiz.domain.gamecreator;

import com.fungame.songquiz.domain.*;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SongGameFactory implements GameFactory {

    private final SongReader songReader;

    @Override
    public GameType getSupportedType() {
        return GameType.SONG;
    }

    @Override
    public Game create(GameCreateInfo info) {
        if (!(info instanceof SongGameCreateInfo(Category category, int songCount))) {
            throw new CoreException(ErrorType.GAME_NOT_FOUND);
        }

        if (category == Category.TOTAL) {
            List<Song> songs = songReader.findSongWithCount(songCount);
            return new SongQuiz(songs, Category.TOTAL);
        }

        List<Song> songs = songReader.findSongByCategoryWithCount(category, songCount);
        return new SongQuiz(songs, category);
    }
}
