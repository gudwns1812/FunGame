package com.fungame.songquiz.controller.request;

import com.fungame.songquiz.enums.ReportStatus;

public record ReportStatusRequest(
        ReportStatus status
) {
}
