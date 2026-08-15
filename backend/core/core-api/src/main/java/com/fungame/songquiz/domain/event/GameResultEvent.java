package com.fungame.songquiz.domain.event;

import com.fungame.songquiz.domain.PlayerScore;

import java.util.List;

public record GameResultEvent(
        Long roomId,
        List<PlayerScore> rankings
) {
}
