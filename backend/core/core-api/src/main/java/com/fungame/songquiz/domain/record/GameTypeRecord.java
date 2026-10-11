package com.fungame.songquiz.domain.record;

import com.fungame.songquiz.enums.GameType;

public record GameTypeRecord(GameType gameType, long played, long won) {

    private static final Integer NO_WIN_RATE = null;
    private static final double PERCENT = 100.0;

    static GameTypeRecord unplayed(GameType gameType) {
        return new GameTypeRecord(gameType, 0, 0);
    }

    public long lost() {
        return played - won;
    }

    public Integer winRate() {
        if (played == 0) {
            return NO_WIN_RATE;
        }
        return (int) Math.round(won * PERCENT / played);
    }
}
