package com.fungame.songquiz.domain.event;

import com.fungame.songquiz.domain.ActionResult;
import com.fungame.songquiz.domain.ActionType;
import com.fungame.songquiz.domain.dto.GameContentDto;

public record HaliGaliActionEvent(
        Long roomId,
        String playerName,
        ActionType actionType,
        ActionResult result,
        GameContentDto status
) {
}
