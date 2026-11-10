package com.fungame.songquiz.domain.quiz;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public record TitleReading(String pronunciation, List<String> aliases) {

    private static final int MAX_ALIASES = 8;

    public TitleReading {
        pronunciation = pronunciation == null ? null : pronunciation.strip();
        aliases = aliases == null ? List.of() : aliases.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(Predicate.not(String::isEmpty))
                .distinct()
                .limit(MAX_ALIASES)
                .toList();
    }

    public boolean isUsable() {
        return pronunciation != null && Choseong.isHangulTitle(pronunciation);
    }
}
