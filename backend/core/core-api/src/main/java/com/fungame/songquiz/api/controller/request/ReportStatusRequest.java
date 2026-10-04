package com.fungame.songquiz.api.controller.request;

import com.fungame.songquiz.enums.ReportStatus;

public record ReportStatusRequest(
        ReportStatus status
) {
}
