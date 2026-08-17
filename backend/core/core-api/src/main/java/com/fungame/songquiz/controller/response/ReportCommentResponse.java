package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.report.ReportComment;

import java.time.LocalDateTime;
import java.util.List;

public record ReportCommentResponse(
        Long id,
        String authorNickname,
        String content,
        LocalDateTime createdAt
) {

    public static ReportCommentResponse from(ReportComment comment) {
        return new ReportCommentResponse(comment.id(), comment.authorNickname(), comment.content(),
                comment.createdAt());
    }

    public static List<ReportCommentResponse> listFrom(List<ReportComment> comments) {
        return comments.stream()
                .map(ReportCommentResponse::from)
                .toList();
    }
}
