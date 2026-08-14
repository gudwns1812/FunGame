package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.gamecreator.GameCreateInfo;
import com.fungame.songquiz.enums.GameType;

public interface GameFactory {
    GameType getSupportedType();

    Game create(GameCreateInfo info);
}
