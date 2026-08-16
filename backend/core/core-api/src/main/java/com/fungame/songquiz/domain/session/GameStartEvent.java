package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.GameInfo;

public record GameStartEvent(
        Long roomId,
        GameInfo gameInfo
) {
}
