package com.fungame.songquiz.domain.event;

import com.fungame.songquiz.domain.dto.GameAnswerDto;

public record RoundEndEvent(
        Long roomId,
        Long winnerMemberId,
        String winnerNickname,
        GameAnswerDto answer
) {
}
