package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.room.GamePlayer;

public record PlayerScore(GamePlayer player, int score, int rank) {

    public Long memberId() {
        return player.memberId();
    }
}
