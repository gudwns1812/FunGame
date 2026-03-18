package com.fungame.songquiz.domain.dto;

import com.fungame.songquiz.domain.ComputerScienceQuizGame;
import com.fungame.songquiz.domain.Game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record GameAnswerDto(
        Game game,
        List<String> data
) {
    public static GameAnswerDto from(Game game, String... answers) {
        var data = new ArrayList<>(Arrays.asList(answers));

        return new GameAnswerDto(game, data);
    }

    @Override
    public String toString() {
        return switch (game) {
            case ComputerScienceQuizGame cs -> String.join(", ", data);
            default -> String.join(" ", data);
        };
    }
}
