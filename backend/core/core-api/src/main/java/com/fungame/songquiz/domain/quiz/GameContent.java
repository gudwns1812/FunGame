package com.fungame.songquiz.domain.quiz;

import java.util.List;

public record GameContent(
        String description,
        List<String> data
) {

    public static GameContent of(String description) {
        return new GameContent(description, List.of(description));
    }
}
