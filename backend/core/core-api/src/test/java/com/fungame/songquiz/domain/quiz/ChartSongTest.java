package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChartSongTest {

    private static final LocalDate RELEASE_DATE = LocalDate.of(2026, 11, 3);
    private static final List<Category> CATEGORIES = List.of(Category.KPOP, Category.DANCE);

    @Test
    @DisplayName("좋아요가 5만 이상이어야 인기곡이다.")
    void popularFromFiftyThousandLikes() {
        assertThat(chartSong("밤양갱", 49_999).isPopular()).isFalse();
        assertThat(chartSong("밤양갱", 50_000).isPopular()).isTrue();
    }

    @Test
    @DisplayName("가수 이름의 NBSP 는 공백으로 바꾸고, 여럿이면 쉼표로 잇는다.")
    void joinsSingers() {
        ChartSong song = ChartSong.of(1L, "NEON", List.of("A2O", "A2O LITE"), 66_733);

        assertThat(song.singer()).isEqualTo("A2O, A2O LITE");
    }

    @Test
    @DisplayName("한글 제목은 읽는 법이 필요 없고, 한글이 아닌 제목은 필요하다.")
    void needsReadingOnlyForNonHangulTitle() {
        assertThat(chartSong("백화요란 (Inside Out)", 50_000).needsReading()).isFalse();
        assertThat(chartSong("Whiplash", 50_000).needsReading()).isTrue();
        assertThat(chartSong("0+0", 50_000).needsReading()).isTrue();
    }

    @Test
    @DisplayName("한글 제목은 제목의 초성을 힌트로, 괄호 속 이름을 정답으로 둔다.")
    void hangulTitleToSong() {
        Song song = chartSong("백화요란 (Inside Out)", 50_000).toSong(RELEASE_DATE, CATEGORIES);

        assertThat(song.getTitle()).isEqualTo("백화요란");
        assertThat(song.getSinger()).isEqualTo("RESCENE (리센느)");
        assertThat(song.getHint()).isEqualTo("ㅂㅎㅇㄹ");
        assertThat(song.getAnswers()).containsExactlyInAnyOrder("백화요란", "insideout");
        assertThat(song.getReleaseDate()).isEqualTo(RELEASE_DATE);
        assertThat(song.getCategories()).isEqualTo(CATEGORIES);
        assertThat(song.getLink()).isNull();
        assertThat(song.getPlaySeconds()).isZero();
    }

    @Test
    @DisplayName("정답은 띄어쓰기 · 문장부호를 빼고 영어를 소문자로 바꿔 넣는다. 채팅 안내가 그렇게 입력하라고 한다.")
    void answersFollowChatInputRule() {
        Song song = chartSong("어떻게 이별까지 사랑하겠어, 널 사랑하는 거지", 50_000).toSong(RELEASE_DATE, CATEGORIES);

        assertThat(song.getAnswers()).contains("어떻게이별까지사랑하겠어널사랑하는거지");
    }

    @Test
    @DisplayName("괄호 속 이름에 쉼표가 있어도 쉼표를 빼고 정답으로 넣는다.")
    void aliasWithCommaBecomesAnswer() {
        Song song = chartSong("모든 날 (Every day, Every Moment)", 50_000).toSong(RELEASE_DATE, CATEGORIES);

        assertThat(song.getAnswers()).containsExactlyInAnyOrder("모든 날", "모든날", "everydayeverymoment");
    }

    @Test
    @DisplayName("한글이 아닌 제목은 독음의 초성을 힌트로 두고, 제목 · 독음 · 비슷한 독음을 모두 정답으로 넣는다.")
    void nonHangulTitleToSong() {
        TitleReading reading = new TitleReading("홈 스윗 홈", List.of("홈스위트홈"));

        Song song = chartSong("HOME SWEET HOME (feat. 태양, 대성)", 50_000).toSong(RELEASE_DATE, CATEGORIES, reading);

        assertThat(song.getTitle()).isEqualTo("HOME SWEET HOME");
        assertThat(song.getHint()).isEqualTo("ㅎㅅㅇㅎ");
        assertThat(song.getAnswers()).containsExactlyInAnyOrder(
                "HOME SWEET HOME", "homesweethome", "홈스윗홈", "홈스위트홈");
    }

    @Test
    @DisplayName("문장부호만 다른 정답은 하나로 친다.")
    void dropsAnswersSameAfterNormalizing() {
        TitleReading reading = new TitleReading("잇츠 미", List.of("잇츠미", "잇츠 미!", "It's me"));

        Song song = chartSong("It′s Me", 50_000).toSong(RELEASE_DATE, CATEGORIES, reading);

        assertThat(song.getAnswers()).containsExactlyInAnyOrder("It's Me", "itsme", "잇츠미");
    }

    @Test
    @DisplayName("한글이 아닌 제목을 읽는 법 없이 곡으로 만들 수 없다.")
    void nonHangulTitleNeedsReading() {
        assertThatThrownBy(() -> chartSong("Whiplash", 50_000).toSong(RELEASE_DATE, CATEGORIES))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("정답을 이어 붙인 길이가 저장 칸(255자)을 넘으면 넘치는 별칭부터 버린다.")
    void keepsAnswersWithinColumnLength() {
        String longAlias = "가".repeat(230);
        TitleReading reading = new TitleReading("위플래시", List.of(longAlias, "위플래쉬"));

        Song song = chartSong("Whiplash", 50_000).toSong(RELEASE_DATE, CATEGORIES, reading);

        assertThat(String.join(",", song.getAnswers()).length()).isLessThanOrEqualTo(255);
        assertThat(song.getAnswers()).contains("Whiplash", "whiplash", "위플래시", longAlias).doesNotContain("위플래쉬");
    }

    private static ChartSong chartSong(String rawTitle, long likes) {
        return ChartSong.of(603712834L, rawTitle, List.of("RESCENE (리센느)"), likes);
    }
}
