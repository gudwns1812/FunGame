package com.fungame.songquiz.domain.report;

import com.fungame.songquiz.enums.ReportReason;
import com.fungame.songquiz.enums.ReportSource;
import com.fungame.songquiz.enums.ReportStatus;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.Getter;

@Getter
public class Report {

    private final Long memberId;
    private final ReportSource source;
    private final ReportReason reason;
    private final String detail;
    private final ReportContext context;
    private final ReportStatus status;

    private Report(Long memberId, ReportSource source, ReportReason reason, String detail, ReportContext context,
                   ReportStatus status) {
        this.memberId = memberId;
        this.source = source;
        this.reason = reason;
        this.detail = detail;
        this.context = context;
        this.status = status;
    }

    public static Report open(Long memberId, ReportSource source, ReportReason reason, String detail,
                              ReportContext context) {
        if (reason.needsDetail() && (detail == null || detail.isBlank())) {
            throw new CoreException(ErrorType.REPORT_DETAIL_REQUIRED);
        }

        return new Report(memberId, source, reason, detail, context, ReportStatus.OPEN);
    }

    public boolean pointsAtContent() {
        return context.contentId() != null;
    }
}
