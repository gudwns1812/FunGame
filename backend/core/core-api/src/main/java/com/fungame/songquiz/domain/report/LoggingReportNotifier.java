package com.fungame.songquiz.domain.report;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
@Profile("!prod")
public class LoggingReportNotifier implements ReportNotifier {

    private static final String LINE_DELIMITER = " | ";

    @Async
    @Override
    public void notifyReport(Report report) {
        ReportSummary summary = ReportSummary.of(report);

        log.info("신고 접수 - {} : {}", summary.title(), summary.lines().stream()
                .map(line -> line.name() + "=" + line.value())
                .collect(Collectors.joining(LINE_DELIMITER)));
    }
}
