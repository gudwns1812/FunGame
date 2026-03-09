package com.fungame.songquiz.domain.gamecreator;

import com.fungame.songquiz.domain.*;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SongGameFactory implements com.fungame.songquiz.domain.GameFactory {

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

        List<Song> songs = songReader.findSongByCategoryWithCount(category, songCount);
        return new SongQuiz(songs, category);
    }
}
