package com.fungame.songquiz.domain.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fungame.songquiz.domain.room.GamePlayer;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameOutcomeTest {

    private static final GamePlayer FIRST = GamePlayer.createNewPlayer(1L);
    private static final GamePlayer SECOND = GamePlayer.createNewPlayer(2L);
    private static final GamePlayer THIRD = GamePlayer.createNewPlayer(3L);
    private static final GamePlayer FOURTH = GamePlayer.createNewPlayer(4L);
    private static final Integer NO_RANK = null;
    private static final Integer NO_SCORE = null;

    @Test
    void 점수제_판은_공동_1위가_모두_이기고_나머지는_진다() {
        var gameRank = new GameRank(List.of(FIRST, SECOND, THIRD));
        scoreTimes(gameRank, FIRST, 2);
        scoreTimes(gameRank, SECOND, 2);
        scoreTimes(gameRank, THIRD, 1);

        assertThat(gameRank.scoredOutcome().players())
                .extracting(PlayerOutcome::memberId, PlayerOutcome::won, PlayerOutcome::rank, PlayerOutcome::score)
                .containsExactly(
                        tuple(FIRST.memberId(), true, 1, 2),
                        tuple(SECOND.memberId(), true, 1, 2),
                        tuple(THIRD.memberId(), false, 3, 1));
    }

    @Test
    void 아무도_못_맞힌_점수제_판은_모두_1위지만_모두_진다() {
        var gameRank = new GameRank(List.of(FIRST, SECOND));

        assertThat(gameRank.scoredOutcome().players())
                .extracting(PlayerOutcome::rank, PlayerOutcome::won)
                .containsExactly(tuple(1, false), tuple(1, false));
    }

    @Test
    void 탈주자는_점수와_상관없이_끝까지_남은_사람들_아래_같은_순위이고_진다() {
        var gameRank = new GameRank(List.of(FIRST, SECOND, THIRD, FOURTH));
        scoreTimes(gameRank, FIRST, 1);
        scoreTimes(gameRank, THIRD, 3);
        scoreTimes(gameRank, FOURTH, 2);
        gameRank.deactivate(THIRD.memberId());
        gameRank.deactivate(FOURTH.memberId());

        assertThat(gameRank.scoredOutcome().players())
                .extracting(PlayerOutcome::memberId, PlayerOutcome::won, PlayerOutcome::rank, PlayerOutcome::score,
                        PlayerOutcome::left)
                .containsExactly(
                        tuple(FIRST.memberId(), true, 1, 1, false),
                        tuple(SECOND.memberId(), false, 2, 0, false),
                        tuple(THIRD.memberId(), false, 3, 3, true),
                        tuple(FOURTH.memberId(), false, 3, 2, true));
    }

    @Test
    void 나갔다가_돌아와_끝까지_남은_사람은_탈주자가_아니다() {
        var gameRank = new GameRank(List.of(FIRST, SECOND));
        scoreTimes(gameRank, SECOND, 1);
        gameRank.deactivate(SECOND.memberId());
        gameRank.activate(SECOND);

        assertThat(gameRank.scoredOutcome().players())
                .extracting(PlayerOutcome::memberId, PlayerOutcome::won, PlayerOutcome::left)
                .containsExactly(
                        tuple(SECOND.memberId(), true, false),
                        tuple(FIRST.memberId(), false, false));
    }

    @Test
    void 성공한_협동_판은_끝까지_남은_사람만_이기고_순위와_점수가_없다() {
        var gameRank = new GameRank(List.of(FIRST, SECOND, THIRD));
        gameRank.deactivate(SECOND.memberId());

        assertThat(gameRank.cooperativeOutcome(true).players())
                .extracting(PlayerOutcome::memberId, PlayerOutcome::won, PlayerOutcome::rank, PlayerOutcome::score,
                        PlayerOutcome::left)
                .containsExactly(
                        tuple(FIRST.memberId(), true, NO_RANK, NO_SCORE, false),
                        tuple(THIRD.memberId(), true, NO_RANK, NO_SCORE, false),
                        tuple(SECOND.memberId(), false, NO_RANK, NO_SCORE, true));
    }

    @Test
    void 실패한_협동_판은_모두_진다() {
        var gameRank = new GameRank(List.of(FIRST, SECOND));

        assertThat(gameRank.cooperativeOutcome(false).players())
                .extracting(PlayerOutcome::won)
                .containsOnly(false);
    }

    @Test
    void 혼자_시작한_판은_전적으로_남기지_않는다() {
        var gameRank = new GameRank(List.of(FIRST));

        assertThat(gameRank.scoredOutcome().isRecordable()).isFalse();
        assertThat(gameRank.cooperativeOutcome(true).isRecordable()).isFalse();
    }

    @Test
    void 둘이_시작했다면_한_명이_나가도_전적으로_남긴다() {
        var gameRank = new GameRank(List.of(FIRST, SECOND));
        gameRank.deactivate(SECOND.memberId());

        assertThat(gameRank.scoredOutcome().isRecordable()).isTrue();
    }

    private static void scoreTimes(GameRank gameRank, GamePlayer player, int times) {
        for (int i = 0; i < times; i++) {
            gameRank.updatePoint(player.memberId());
        }
    }
}
