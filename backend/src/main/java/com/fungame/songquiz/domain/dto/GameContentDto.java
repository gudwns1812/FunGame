package com.fungame.songquiz.domain.dto;

import com.fungame.songquiz.domain.ComputerScienceQuizGame;
import com.fungame.songquiz.domain.Game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record GameContentDto(
        Game game,
        List<String> data
) {
    public static GameContentDto from(Game game, String... answers) {
        var data = new ArrayList<>(Arrays.asList(answers));

        return new GameContentDto(game, data);
    }

    @Override
    public String toString() {
        return switch (game) {
            case ComputerScienceQuizGame cs -> "분류: " + data.get(0) + ", 난이도: " + data.get(1) + ", 질문: " + data.get(2);
            default -> data.getFirst();
        };
    }
}
