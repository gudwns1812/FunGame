package com.fungame.songquiz.storage;

import com.fungame.songquiz.enums.GameType;
import java.time.Instant;

public record GameRecordRow(
        String gameId,
        GameType gameType,
        Instant startedAt,
        Instant endedAt,
        int startedPlayerCount
) {
}
