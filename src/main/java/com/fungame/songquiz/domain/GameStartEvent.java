package com.fungame.songquiz.domain;

import java.util.List;

public record GameStartEvent(
        String roomId,
        List<Long> songIds
) {
}
