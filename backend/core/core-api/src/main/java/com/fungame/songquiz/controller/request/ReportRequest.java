package com.fungame.songquiz.controller.request;

import com.fungame.songquiz.domain.report.ReportCommand;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.enums.ReportReason;
import com.fungame.songquiz.enums.ReportSource;

public record ReportRequest(
        ReportSource source,
        Long roomId,
        ReportReason reason,
        String detail,
        GameType gameType
) {

    public ReportCommand toCommand() {
        return new ReportCommand(source, roomId, reason, detail, gameType);
    }
}
