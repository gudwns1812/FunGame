package com.fungame.songquiz.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class GameRankTest {

    @Test
    void 게임랭킹은_점수를_내림차순으로_반환한다() {
        //given
        var players = List.of("hi", "park", "jack");
        var gameRank = new GameRank(players);
        //when
        gameRank.updatePoint("park");
        //then
        assertThat(gameRank.getPlayerScores())
                .hasSize(3)
                .extracting(PlayerScore::score)
                .containsExactly(1, 0, 0);
    }
}
