package com.fungame.songquiz.domain.room;

public record GamePlayer(
        Long memberId,
        boolean isReady
) {

    public static GamePlayer createNewPlayer(Long memberId) {
        return new GamePlayer(memberId, false);
    }

    public GamePlayer toggleReady() {
        return new GamePlayer(memberId, !isReady);
    }

    public GamePlayer setReady(boolean ready) {
        return new GamePlayer(memberId, ready);
    }
}
