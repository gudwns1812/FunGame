package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.room.GamePlayer;

public record PlayerScore(GamePlayer player, int score, int rank) {

    private static final int TOP_RANK = 1;
    private static final int NO_SCORE = 0;

    public Long memberId() {
        return player.memberId();
    }

    public boolean isWinner() {
        return rank == TOP_RANK && score > NO_SCORE;
    }
}
