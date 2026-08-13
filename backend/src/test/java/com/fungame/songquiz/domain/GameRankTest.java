package com.fungame.songquiz.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameRankTest {

    @Test
    void 게임랭킹은_점수를_내림차순으로_반환한다() {
        //given
        var park = GamePlayer.createNewPlayer(2L, "park");
        var players = List.of(
                GamePlayer.createNewPlayer(1L, "hi"),
                park,
                GamePlayer.createNewPlayer(3L, "jack"));
        var gameRank = new GameRank(players);
        //when
        gameRank.updatePoint(park.memberId());
        //then
        assertThat(gameRank.getPlayerScores())
                .hasSize(3)
                .extracting(PlayerScore::score)
                .containsExactly(1, 0, 0);
    }
}
