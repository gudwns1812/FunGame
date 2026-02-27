package com.fungame.songquiz.domain.event;

import java.util.Map;

public record GameEndEvent(
        String roomId,
        Map<String, Integer> rankings
) {
}
