package com.fungame.songquiz.domain.event;

import com.fungame.songquiz.domain.dto.GameAnswerDto;

public record RoundEndEvent(
        Long roomId,
        String winner,
        GameAnswerDto answer
) {

}
