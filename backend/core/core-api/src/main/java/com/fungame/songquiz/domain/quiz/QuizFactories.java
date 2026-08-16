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
public class QuizFactories {

    private final Map<GameType, QuizFactory> factories;

    public QuizFactories(List<QuizFactory> factories) {
        this.factories = factories.stream()
                .collect(Collectors.toMap(QuizFactory::getSupportedType, Function.identity()));
    }

    public Quiz create(RoomSettings settings) {
        QuizFactory factory = factories.get(settings.gameType());
        if (factory == null) {
            throw new CoreException(ErrorType.GAME_NOT_FOUND);
        }

        return factory.create(settings.toQuizCreateInfo());
    }
}
