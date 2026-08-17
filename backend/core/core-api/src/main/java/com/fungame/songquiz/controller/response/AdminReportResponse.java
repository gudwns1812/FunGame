package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.report.Report;
import com.fungame.songquiz.domain.report.ReportContext;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.enums.ReportReason;
import com.fungame.songquiz.enums.ReportSource;
import com.fungame.songquiz.enums.ReportStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AdminReportResponse(
        Long id,
        Long memberId,
        String reporterNickname,
        ReportSource source,
        ReportReason reason,
        String detail,
        GameType gameType,
        String quizCategory,
        Long contentId,
        Long roomId,
        Integer currentRound,
        Integer totalRound,
        String quizContent,
        String quizAnswer,
        String quizHint,
        ReportStatus status,
        LocalDateTime createdAt,
        List<ReportCommentResponse> comments
) {

    public static AdminReportResponse from(Report report) {
        ReportContext context = report.getContext();

        return new AdminReportResponse(
                report.getId(),
                report.getMemberId(),
                report.getReporterNickname(),
                report.getSource(),
                report.getReason(),
                report.getDetail(),
                context.gameType(),
                context.category(),
                context.contentId(),
                context.roomId(),
                context.currentRound(),
                context.totalRound(),
                context.content(),
                context.answer(),
                context.hint(),
                report.getStatus(),
                report.getCreatedAt(),
                ReportCommentResponse.listFrom(report.getComments()));
    }

    public static List<AdminReportResponse> listFrom(List<Report> reports) {
        return reports.stream()
                .map(AdminReportResponse::from)
                .toList();
    }
}
