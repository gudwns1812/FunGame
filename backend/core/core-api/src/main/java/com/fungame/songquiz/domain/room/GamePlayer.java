package com.fungame.songquiz.domain.room;

public record GamePlayer(
        Long memberId,
        String nickname,
        boolean isReady
) {

    public static GamePlayer createNewPlayer(Long memberId, String nickname) {
        return new GamePlayer(memberId, nickname, false);
    }

    public GamePlayer toggleReady() {
        return new GamePlayer(memberId, nickname, !isReady);
    }

    public GamePlayer setReady(boolean ready) {
        return new GamePlayer(memberId, nickname, ready);
    }
}
