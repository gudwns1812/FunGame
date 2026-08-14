package com.fungame.songquiz.domain.event;

import com.fungame.songquiz.domain.dto.GameContentDto;

public record RoundStartEvent(
        Long roomId,
        GameContentDto content,
        int currentRound,
        int totalRound
) {
}
