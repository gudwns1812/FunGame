package com.fungame.songquiz.domain.quiz;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SongTitleTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "HOME SWEET HOME (feat. 태양, 대성)|HOME SWEET HOME",
            "삐그덕 (Feat. 아이유)|삐그덕",
            "모르시나요(PROD.로코베리)|모르시나요",
            "퇴사할게여 (Narr. 기안84)|퇴사할게여",
            "봄 (With 아이유)|봄",
    })
    @DisplayName("참여 표기 괄호는 떼고 별칭으로도 쓰지 않는다.")
    void dropsCreditParentheses(String raw, String name) {
        SongTitle title = SongTitle.parse(raw);

        assertThat(title.name()).isEqualTo(name);
        assertThat(title.aliases()).isEmpty();
    }

    @Test
    @DisplayName("버전 꼬리와 참여 표기를 함께 뗀다.")
    void dropsVersionSuffix() {
        assertThat(SongTitle.parse("Seven (feat. Latto) - Clean Ver.").name()).isEqualTo("Seven");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "백화요란 (Inside Out)|백화요란|Inside Out",
            "뛰어(JUMP)|뛰어|JUMP",
            "404 (New Era)|404|New Era",
    })
    @DisplayName("괄호 속 다른 이름은 떼어 별칭으로 둔다.")
    void keepsAlternativeTitleAsAlias(String raw, String name, String alias) {
        SongTitle title = SongTitle.parse(raw);

        assertThat(title.name()).isEqualTo(name);
        assertThat(title.aliases()).containsExactly(alias);
    }

    @Test
    @DisplayName("괄호 속에 괄호가 또 있으면 떼기만 하고 별칭으로 쓰지 않는다.")
    void ignoresNestedParentheses() {
        SongTitle title = SongTitle.parse("그대만 있다면 (여름날 우리 X 너드커넥션 (Nerd Connection))");

        assertThat(title.name()).isEqualTo("그대만 있다면");
        assertThat(title.aliases()).isEmpty();
    }

    @Test
    @DisplayName("괄호로 시작하는 제목은 건드리지 않는다.")
    void keepsTitleStartingWithParenthesis() {
        assertThat(SongTitle.parse("(여자)아이들").name()).isEqualTo("(여자)아이들");
    }

    @Test
    @DisplayName("프라임 · 둥근 따옴표는 작은따옴표로, NBSP 는 공백으로 바꾼다.")
    void normalizesLookAlikeCharacters() {
        assertThat(SongTitle.parse("It′s Me").name()).isEqualTo("It's Me");
        assertThat(SongTitle.parse("Don’t Stop").name()).isEqualTo("Don't Stop");
    }

    @Test
    @DisplayName("괄호가 없으면 그대로 둔다.")
    void keepsPlainTitle() {
        SongTitle title = SongTitle.parse("어떻게 이별까지 사랑하겠어, 널 사랑하는 거지");

        assertThat(title.name()).isEqualTo("어떻게 이별까지 사랑하겠어, 널 사랑하는 거지");
        assertThat(title.aliases()).isEmpty();
    }
}
