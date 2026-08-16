package com.fungame.songquiz.domain.session;

import java.util.List;

public record GameStateDto(
        String gameType,
        String category,
        int totalCount,
        int currentRound,
        int totalRound,
        String content,
        List<String> statusData
) {
}
