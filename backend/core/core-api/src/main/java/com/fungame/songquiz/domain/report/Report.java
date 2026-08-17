package com.fungame.songquiz.domain.report;

import com.fungame.songquiz.enums.ReportReason;
import com.fungame.songquiz.enums.ReportSource;
import com.fungame.songquiz.enums.ReportStatus;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class Report {

    private final Long id;
    private final Long memberId;
    private final String reporterNickname;
    private final ReportSource source;
    private final ReportReason reason;
    private final String detail;
    private final ReportContext context;
    private final LocalDateTime createdAt;
    private final List<ReportComment> comments;
    private ReportStatus status;

    private Report(Long id, Long memberId, String reporterNickname, ReportSource source, ReportReason reason,
                   String detail, ReportContext context, ReportStatus status, LocalDateTime createdAt,
                   List<ReportComment> comments) {
        this.id = id;
        this.memberId = memberId;
        this.reporterNickname = reporterNickname;
        this.source = source;
        this.reason = reason;
        this.detail = detail;
        this.context = context;
        this.status = status;
        this.createdAt = createdAt;
        this.comments = comments;
    }

    public static Report open(Long memberId, ReportSource source, ReportReason reason, String detail,
                              ReportContext context) {
        if (reason.needsDetail() && (detail == null || detail.isBlank())) {
            throw new CoreException(ErrorType.REPORT_DETAIL_REQUIRED);
        }

        return new Report(null, memberId, null, source, reason, detail, context, ReportStatus.OPEN, null, List.of());
    }

    public static Report restore(Long id, Long memberId, String reporterNickname, ReportSource source,
                                 ReportReason reason, String detail, ReportContext context, ReportStatus status,
                                 LocalDateTime createdAt, List<ReportComment> comments) {
        return new Report(id, memberId, reporterNickname, source, reason, detail, context, status, createdAt,
                List.copyOf(comments));
    }

    public Report withId(Long id) {
        return new Report(id, memberId, reporterNickname, source, reason, detail, context, status, createdAt, comments);
    }

    public void changeStatus(ReportStatus status) {
        this.status = status;
    }

    public boolean pointsAtContent() {
        return context.contentId() != null;
    }
}
