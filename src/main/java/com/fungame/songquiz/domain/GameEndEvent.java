package com.fungame.songquiz.domain;

import java.util.Map;

public record GameEndEvent(
        String roomId,
        Map<String, Integer> rankings
) {
}
