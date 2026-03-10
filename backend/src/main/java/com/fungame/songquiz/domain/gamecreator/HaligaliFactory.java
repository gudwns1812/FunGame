package com.fungame.songquiz.domain.gamecreator;

import com.fungame.songquiz.domain.Game;
import com.fungame.songquiz.domain.GameFactory;
import com.fungame.songquiz.domain.GameType;
import com.fungame.songquiz.domain.HaliGaliGame;
import org.springframework.stereotype.Component;

@Component
public class HaligaliFactory implements GameFactory {

    @Override
    public GameType getSupportedType() {
        return GameType.HALLIGALLI;
    }

    @Override
    public Game create(GameCreateInfo info) {
        return new HaliGaliGame();
    }
}
