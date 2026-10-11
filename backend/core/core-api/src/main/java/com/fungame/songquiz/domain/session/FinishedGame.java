package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.enums.GameType;
import java.time.Instant;

public record FinishedGame(String gameId, GameType gameType, Instant startedAt, GameOutcome outcome) {
}
