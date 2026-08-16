package com.fungame.songquiz.domain.session;


import java.util.List;

public record GameResultEvent(
        Long roomId,
        List<PlayerScore> rankings
) {
}
