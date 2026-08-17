package com.fungame.songquiz.domain.report;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;

import java.time.LocalDateTime;

public record ReportComment(
        Long id,
        Long reportId,
        Long authorId,
        String authorNickname,
        String content,
        LocalDateTime createdAt
) {

    public static ReportComment write(Long reportId, Long authorId, String content) {
        if (content == null || content.isBlank()) {
            throw new CoreException(ErrorType.REPORT_COMMENT_REQUIRED);
        }

        return new ReportComment(null, reportId, authorId, null, content.trim(), null);
    }
}
