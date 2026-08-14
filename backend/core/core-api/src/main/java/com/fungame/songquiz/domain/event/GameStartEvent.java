package com.fungame.songquiz.domain.event;

import com.fungame.songquiz.domain.dto.GameInfo;

public record GameStartEvent(
        Long roomId,
        GameInfo gameInfo
) {
}
