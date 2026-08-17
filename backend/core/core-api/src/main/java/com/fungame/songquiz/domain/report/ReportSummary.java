package com.fungame.songquiz.domain.report;

import com.fungame.songquiz.enums.ReportReason;

import java.util.ArrayList;
import java.util.List;

public record ReportSummary(
        String title,
        List<Line> lines
) {

    public record Line(
            String name,
            String value
    ) {
    }

    public static ReportSummary of(Report report) {
        return new ReportSummary(titleOf(report.getReason()), linesOf(report));
    }

    private static String titleOf(ReportReason reason) {
        return switch (reason) {
            case CONTENT_NOT_SHOWN -> "문제가 나오지 않는다";
            case CONTENT_WRONG -> "문제가 이상하다";
            case HINT_WRONG -> "힌트가 이상하다";
            case ANSWER_WRONG -> "답이 이상하다";
            case ETC -> "기타 문의";
        };
    }

    private static List<Line> linesOf(Report report) {
        ReportContext context = report.getContext();
        List<Line> lines = new ArrayList<>();

        addLine(lines, "신고 번호", report.getId());
        addLine(lines, "신고자", report.getMemberId());
        addLine(lines, "접수 위치", report.getSource());
        addLine(lines, "게임", context.gameType());
        addLine(lines, "카테고리", context.category());
        addLine(lines, "방", context.roomId());
        addLine(lines, "라운드", roundOf(context));
        addLine(lines, "문제 식별자", context.contentId());
        addLine(lines, "문제", context.content());
        addLine(lines, "정답", context.answer());
        addLine(lines, "힌트", context.hint());
        addLine(lines, "직접 작성", report.getDetail());

        return List.copyOf(lines);
    }

    private static String roundOf(ReportContext context) {
        if (context.currentRound() == null || context.totalRound() == null) {
            return null;
        }

        return context.currentRound() + " / " + context.totalRound();
    }

    private static void addLine(List<Line> lines, String name, Object value) {
        if (value == null || value.toString().isBlank()) {
            return;
        }

        lines.add(new Line(name, value.toString()));
    }
}
