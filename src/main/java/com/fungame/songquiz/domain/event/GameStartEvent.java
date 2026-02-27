package com.fungame.songquiz.domain.event;

import java.util.List;

public record GameStartEvent(
        String roomId,
        List<Long> songIds
) {
}
