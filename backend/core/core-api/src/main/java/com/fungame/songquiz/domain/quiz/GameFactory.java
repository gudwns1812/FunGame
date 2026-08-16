package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.GameType;

public interface GameFactory {
    GameType getSupportedType();

    Game create(GameCreateInfo info);
}
