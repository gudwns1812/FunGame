package com.fungame.songquiz.domain.quiz;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ChoseongTest {

    @Test
    @DisplayName("한글 음절을 초성으로 바꾼다.")
    void extractsInitialConsonants() {
        assertThat(Choseong.of("밤양갱")).isEqualTo("ㅂㅇㄱ");
    }

    @Test
    @DisplayName("쌍자음 초성은 쌍자음 그대로 둔다.")
    void keepsDoubleConsonants() {
        assertThat(Choseong.of("꿈꾸던 또또")).isEqualTo("ㄲㄲㄷㄸㄸ");
    }

    @Test
    @DisplayName("공백은 빼고 숫자 · 문장부호는 그대로 둔다.")
    void dropsSpacesKeepsOtherNonSyllables() {
        assertThat(Choseong.of("어떻게 이별까지 사랑하겠어, 널 사랑하는 거지"))
                .isEqualTo("ㅇㄸㄱㅇㅂㄲㅈㅅㄹㅎㄱㅇ,ㄴㅅㄹㅎㄴㄱㅈ");
        assertThat(Choseong.of("꿈꾸던 어른이 되었나요?")).isEqualTo("ㄲㄲㄷㅇㄹㅇㄷㅇㄴㅇ?");
        assertThat(Choseong.of("2002년의 우리")).isEqualTo("2002ㄴㅇㅇㄹ");
    }

    @ParameterizedTest
    @ValueSource(strings = {"밤양갱", "기쁨, 슬픔, 아름다운 마음", "꿈꾸던 어른이 되었나요?", "2002년의 우리"})
    @DisplayName("글자가 모두 한글 음절이면 한글 제목이다.")
    void hangulTitle(String title) {
        assertThat(Choseong.isHangulTitle(title)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"APT.", "오늘만 I LOVE YOU", "Love wins all", "0+0", "404", " "})
    @DisplayName("한글이 아닌 글자가 섞였거나 한글 음절이 하나도 없으면 한글 제목이 아니다.")
    void notHangulTitle(String title) {
        assertThat(Choseong.isHangulTitle(title)).isFalse();
    }
}
