package com.fungame.songquiz.domain.report;

import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.enums.ReportReason;
import com.fungame.songquiz.enums.ReportSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReportSummaryTest {

    private static final Long MEMBER_ID = 3L;
    private static final Long ROOM_ID = 42L;
    private static final Long CONTENT_ID = 777L;

    private static Report roundRunningReport() {
        return Report.open(MEMBER_ID, ReportSource.IN_GAME, ReportReason.HINT_WRONG, null,
                new ReportContext(GameType.SONG, "KPOP", CONTENT_ID, ROOM_ID, 2, 5,
                        "https://youtu.be/BzYnNdJhZQw", "아이유 - 밤편지", "아이유 - ㅂㅍㅈ"));
    }

    private static Report lobbyReport() {
        return Report.open(MEMBER_ID, ReportSource.LOBBY, ReportReason.ETC, "로그인하면 가끔 튕겨요",
                ReportContext.outsideGame(null, null));
    }

    @Test
    @DisplayName("사유가 제목이 된다.")
    void makesReasonTheTitle() {
        assertThat(ReportSummary.of(roundRunningReport()).title()).contains("힌트");
    }

    @Test
    @DisplayName("라운드 진행 중 신고는 문제와 정답, 힌트까지 담는다.")
    void carriesWholeContextWhileRoundRunning() {
        ReportSummary summary = ReportSummary.of(roundRunningReport());

        assertThat(summary.lines())
                .extracting(ReportSummary.Line::value)
                .contains(String.valueOf(CONTENT_ID),
                        String.valueOf(ROOM_ID),
                        "2 / 5",
                        "https://youtu.be/BzYnNdJhZQw",
                        "아이유 - 밤편지",
                        "아이유 - ㅂㅍㅈ");
    }

    @Test
    @DisplayName("없는 값은 줄로 만들지 않는다.")
    void skipsMissingValues() {
        ReportSummary summary = ReportSummary.of(lobbyReport());

        assertThat(summary.lines())
                .extracting(ReportSummary.Line::value)
                .doesNotContainNull();
        assertThat(summary.lines())
                .extracting(ReportSummary.Line::name)
                .doesNotContain("문제", "정답", "힌트", "방");
    }

    @Test
    @DisplayName("직접 작성한 내용은 그대로 담는다.")
    void carriesWrittenDetail() {
        ReportSummary summary = ReportSummary.of(lobbyReport());

        assertThat(summary.lines())
                .extracting(ReportSummary.Line::value)
                .contains("로그인하면 가끔 튕겨요");
    }

    @Test
    @DisplayName("누가 신고했는지 담는다.")
    void carriesReporter() {
        ReportSummary summary = ReportSummary.of(lobbyReport());

        assertThat(summary.lines())
                .extracting(ReportSummary.Line::value)
                .contains(String.valueOf(MEMBER_ID));
    }
}
