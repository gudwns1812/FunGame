package com.fungame.songquiz.domain.quiz;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TitleReadingTest {

    @Test
    @DisplayName("독음이 한글로만 되어 있어야 쓸 수 있다.")
    void usableOnlyWhenPronunciationIsHangul() {
        assertThat(new TitleReading("위플래시", List.of()).isUsable()).isTrue();
        assertThat(new TitleReading("Whiplash", List.of()).isUsable()).isFalse();
        assertThat(new TitleReading("위플래시 lash", List.of()).isUsable()).isFalse();
        assertThat(new TitleReading(" ", List.of()).isUsable()).isFalse();
        assertThat(new TitleReading(null, List.of()).isUsable()).isFalse();
    }

    @Test
    @DisplayName("별칭은 앞뒤 공백을 지우고 빈 값 · 중복을 빼서 8개까지만 둔다.")
    void normalizesAliases() {
        TitleReading reading = new TitleReading("에이피티",
                Arrays.asList(" 아파트 ", "", null, "아파트", "apt", "에이 피 티", "아파트!", "a.p.t",
                        "아팥", "아빠트", "에이피티", "apt."));

        assertThat(reading.aliases()).containsExactly(
                "아파트", "apt", "에이 피 티", "아파트!", "a.p.t", "아팥", "아빠트", "에이피티");
    }

    @Test
    @DisplayName("별칭이 없으면 빈 목록이다.")
    void nullAliases() {
        assertThat(new TitleReading("위플래시", null).aliases()).isEmpty();
    }
}
