package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MelonGenreTest {

    @Test
    @DisplayName("POP 이 있으면 POP 하나만 준다.")
    void pop() {
        assertThat(MelonGenre.categoriesOf(List.of("POP"))).containsExactly(Category.POP);
        assertThat(MelonGenre.categoriesOf(List.of("록/메탈", "POP"))).containsExactly(Category.POP);
    }

    @Test
    @DisplayName("J-POP 이 있으면 JPOP 하나만 준다.")
    void jpop() {
        assertThat(MelonGenre.categoriesOf(List.of("J-POP"))).containsExactly(Category.JPOP);
    }

    @ParameterizedTest
    @CsvSource({
            "발라드, BALLAD",
            "댄스, DANCE",
            "랩/힙합, RAP",
            "R&B/Soul, RNB",
            "록/메탈, ROCK",
            "인디음악, ROCK",
            "국내드라마, OST",
            "국내영화, OST",
            "애니메이션/웹툰, OST",
    })
    @DisplayName("국내 곡은 KPOP 에 장르 카테고리를 더한다.")
    void domesticGenre(String genre, Category category) {
        assertThat(MelonGenre.categoriesOf(List.of(genre))).containsExactly(Category.KPOP, category);
    }

    @Test
    @DisplayName("장르가 여럿이면 대응하는 카테고리를 모두 준다.")
    void severalGenres() {
        assertThat(MelonGenre.categoriesOf(List.of("발라드", "국내드라마")))
                .containsExactly(Category.KPOP, Category.BALLAD, Category.OST);
    }

    @Test
    @DisplayName("대응하는 카테고리가 없는 장르는 KPOP 만 준다.")
    void unknownGenre() {
        assertThat(MelonGenre.categoriesOf(List.of("트로트"))).containsExactly(Category.KPOP);
        assertThat(MelonGenre.categoriesOf(List.of())).containsExactly(Category.KPOP);
    }
}
