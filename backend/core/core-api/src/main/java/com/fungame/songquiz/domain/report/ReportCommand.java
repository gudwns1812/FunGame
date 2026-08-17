package com.fungame.songquiz.domain.report;

import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.enums.ReportReason;
import com.fungame.songquiz.enums.ReportSource;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;

public record ReportCommand(
        ReportSource source,
        Long roomId,
        ReportReason reason,
        String detail,
        GameType gameType
) {

    public ReportCommand {
        if (source == null || reason == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (source.needsRoom() != (roomId != null)) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
    }
}
