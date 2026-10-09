package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.room.GamePlayer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class GameRankTest {

    private static final GamePlayer FIRST = GamePlayer.createNewPlayer(1L);
    private static final GamePlayer SECOND = GamePlayer.createNewPlayer(2L);
    private static final GamePlayer THIRD = GamePlayer.createNewPlayer(3L);

    @Test
    void 게임랭킹은_점수를_내림차순으로_반환한다() {
        //given
        var gameRank = new GameRank(List.of(FIRST, SECOND, THIRD));
        //when
        gameRank.updatePoint(SECOND.memberId());
        //then
        assertThat(gameRank.getPlayerScores())
                .hasSize(3)
                .extracting(PlayerScore::score)
                .containsExactly(1, 0, 0);
    }

    @Test
    void 점수가_같으면_가입_순서와_상관없이_같은_순위를_받는다() {
        var gameRank = new GameRank(List.of(FIRST, SECOND, THIRD));

        gameRank.updatePoint(THIRD.memberId());
        gameRank.updatePoint(SECOND.memberId());

        assertThat(gameRank.getPlayerScores())
                .extracting(PlayerScore::memberId, PlayerScore::rank)
                .containsExactly(
                        tuple(SECOND.memberId(), 1),
                        tuple(THIRD.memberId(), 1),
                        tuple(FIRST.memberId(), 3));
    }

    @Test
    void 동점자_다음_순위는_동점_인원만큼_건너뛴다() {
        var fourth = GamePlayer.createNewPlayer(4L);
        var gameRank = new GameRank(List.of(FIRST, SECOND, THIRD, fourth));

        gameRank.updatePoint(FIRST.memberId());
        gameRank.updatePoint(FIRST.memberId());
        gameRank.updatePoint(SECOND.memberId());
        gameRank.updatePoint(THIRD.memberId());

        assertThat(gameRank.getPlayerScores())
                .extracting(PlayerScore::rank)
                .containsExactly(1, 2, 2, 4);
    }

    @Test
    void 아무도_점수를_얻지_못하면_전원이_1위다() {
        var gameRank = new GameRank(List.of(FIRST, SECOND, THIRD));

        assertThat(gameRank.getPlayerScores())
                .extracting(PlayerScore::rank)
                .containsOnly(1);
    }

    @Test
    void 떠난_참가자는_순위를_매길_때_빠진다() {
        var gameRank = new GameRank(List.of(FIRST, SECOND, THIRD));
        gameRank.updatePoint(FIRST.memberId());
        gameRank.updatePoint(FIRST.memberId());
        gameRank.updatePoint(SECOND.memberId());

        gameRank.deactivate(FIRST.memberId());

        assertThat(gameRank.getPlayerScores())
                .extracting(PlayerScore::memberId, PlayerScore::rank)
                .containsExactly(
                        tuple(SECOND.memberId(), 1),
                        tuple(THIRD.memberId(), 2));
    }
}
