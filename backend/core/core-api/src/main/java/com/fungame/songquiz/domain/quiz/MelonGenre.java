package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.Category;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MelonGenre {

    private static final String POP = "POP";
    private static final String JPOP = "J-POP";
    private static final Map<String, Category> DOMESTIC = Map.of(
            "발라드", Category.BALLAD,
            "댄스", Category.DANCE,
            "랩/힙합", Category.RAP,
            "R&B/Soul", Category.RNB,
            "록/메탈", Category.ROCK,
            "인디음악", Category.ROCK,
            "국내드라마", Category.OST,
            "국내영화", Category.OST,
            "애니메이션/웹툰", Category.OST);

    private MelonGenre() {
    }

    public static List<Category> categoriesOf(List<String> genres) {
        if (genres.contains(POP)) {
            return List.of(Category.POP);
        }
        if (genres.contains(JPOP)) {
            return List.of(Category.JPOP);
        }

        Set<Category> categories = new LinkedHashSet<>();
        categories.add(Category.KPOP);
        genres.stream()
                .map(DOMESTIC::get)
                .filter(category -> category != null)
                .forEach(categories::add);

        return List.copyOf(categories);
    }
}
