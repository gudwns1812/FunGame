package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.room.GamePlayer;

record Participant(GamePlayer player, int score, boolean playing) {

    static Participant joining(GamePlayer player) {
        return new Participant(player, 0, true);
    }

    Participant scored() {
        return new Participant(player, score + 1, playing);
    }

    Participant left() {
        return new Participant(player, score, false);
    }

    Participant returnedAs(GamePlayer player) {
        return new Participant(player, score, true);
    }

    String nickname() {
        return player.nickname();
    }

    PlayerScore toPlayerScore() {
        return new PlayerScore(player, score);
    }
}
