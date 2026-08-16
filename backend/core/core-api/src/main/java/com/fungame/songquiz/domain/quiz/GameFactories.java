package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.domain.room.RoomSettings;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GameFactories {

    private final Map<GameType, GameFactory> factories;

    public GameFactories(List<GameFactory> factories) {
        this.factories = factories.stream()
                .collect(Collectors.toMap(GameFactory::getSupportedType, Function.identity()));
    }

    public Game create(RoomSettings settings) {
        GameFactory factory = factories.get(settings.gameType());
        if (factory == null) {
            throw new CoreException(ErrorType.GAME_NOT_FOUND);
        }

        return factory.create(settings.toGameCreateInfo());
    }
}
