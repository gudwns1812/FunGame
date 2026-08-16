package com.fungame.songquiz.domain.quiz;


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

    public String explanation() {
        if (data.size() <= 1) {
            return "";
        }
        return String.join(" ", data.subList(1, data.size()));
    }

    public String getAnswer() {
        return switch (game) {
            case ComputerScienceQuizGame cs -> data.getFirst();
            default -> String.join(" ", data);
        };
    }
}
