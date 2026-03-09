package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.gamecreator.GameCreateInfo;

public interface GameFactory {
    GameType getSupportedType();

    Game create(GameCreateInfo info);
}
