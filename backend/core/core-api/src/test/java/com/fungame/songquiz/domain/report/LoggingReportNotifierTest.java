package com.fungame.songquiz.domain.report;

import com.fungame.songquiz.enums.ReportReason;
import com.fungame.songquiz.enums.ReportSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class LoggingReportNotifierTest {

    private final LoggingReportNotifier notifier = new LoggingReportNotifier();

    @Test
    @DisplayName("게임 컨텍스트가 비어 있는 신고도 남긴다.")
    void logsReportWithoutContext() {
        Report report = Report.open(1L, ReportSource.LOBBY, ReportReason.ETC, "튕겨요",
                ReportContext.outsideGame(null, null));

        assertThatCode(() -> notifier.notifyReport(report)).doesNotThrowAnyException();
    }
}
