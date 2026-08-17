package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.report.Report;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.enums.ReportReason;
import com.fungame.songquiz.enums.ReportSource;
import com.fungame.songquiz.enums.ReportStatus;

import java.time.LocalDateTime;
import java.util.List;

public record MyReportResponse(
        Long id,
        ReportSource source,
        ReportReason reason,
        String detail,
        GameType gameType,
        ReportStatus status,
        LocalDateTime createdAt,
        List<ReportCommentResponse> comments
) {

    public static MyReportResponse from(Report report) {
        return new MyReportResponse(
                report.getId(),
                report.getSource(),
                report.getReason(),
                report.getDetail(),
                report.getContext().gameType(),
                report.getStatus(),
                report.getCreatedAt(),
                ReportCommentResponse.listFrom(report.getComments()));
    }

    public static List<MyReportResponse> listFrom(List<Report> reports) {
        return reports.stream()
                .map(MyReportResponse::from)
                .toList();
    }
}
