package com.fungame.songquiz.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GameSessionRejoinTest {

    private static final GamePlayer P1 = GamePlayer.createNewPlayer(1L, "p1");
    private static final GamePlayer P2 = GamePlayer.createNewPlayer(2L, "p2");
    private static final GamePlayer P3 = GamePlayer.createNewPlayer(3L, "p3");
    private static final Long OUTSIDER_ID = 99L;

    private GameSession quizSession(GamePlayer... players) {
        SongQuiz game = new SongQuiz(List.of(mock(Song.class)), Category.KPOP);
        return new GameSession(game, Arrays.asList(players));
    }

    @Test
    @DisplayName("이탈했던 참가자는 재입장할 수 있고, 이탈 전 점수를 그대로 이어받는다.")
    void rejoin_restores_score() {
        // given
        GameSession session = quizSession(P1, P2);
        session.updatePlayerPoint(P1.memberId());
        session.updatePlayerPoint(P1.memberId());
        session.removePlayer(P1.memberId());

        // then: 이탈 중에는 순위에서 빠진다
        assertThat(session.getPlayerRanks()).extracting(PlayerScore::memberId).containsExactly(P2.memberId());
        assertThat(session.canRejoin(P1.memberId())).isTrue();

        // when
        session.restorePlayer(P1);

        // then: 점수가 보존된 채 복귀한다
        assertThat(session.getPlayerRanks()).containsExactly(
                new PlayerScore(P1.memberId(), "p1", 2),
                new PlayerScore(P2.memberId(), "p2", 0));
        assertThat(session.canRejoin(P1.memberId())).isFalse();
    }

    @Test
    @DisplayName("이 게임에 참가한 적 없는 사람은 재입장할 수 없다.")
    void non_participant_cannot_rejoin() {
        GameSession session = quizSession(P1, P2);

        assertThat(session.canRejoin(OUTSIDER_ID)).isFalse();
    }

    @Test
    @DisplayName("접속 중인 참가자는 재입장 대상이 아니다.")
    void active_player_is_not_rejoin_target() {
        GameSession session = quizSession(P1, P2);

        assertThat(session.canRejoin(P1.memberId())).isFalse();
    }

    @Test
    @DisplayName("행맨 재입장자는 턴 순서 맨 뒤에 붙어 진행 중인 차례를 흐트러뜨리지 않는다.")
    void hangman_rejoin_appends_to_turn_order() {
        // given: p1, p2, p3 중 p2 차례
        HangmanGame game = HangmanGame.create("APPLE");
        game.initPlayers(List.of(P1, P2, P3));
        GameSession session = new GameSession(game, List.of(P1, P2, P3));
        game.guess(P1.memberId(), 'A');
        assertThat(game.getCurrentTurnPlayer().memberId()).isEqualTo(P2.memberId());

        // when: p1 이 나갔다 돌아온다
        session.removePlayer(P1.memberId());
        assertThat(game.getCurrentTurnPlayer().memberId()).isEqualTo(P2.memberId());
        session.restorePlayer(P1);

        // then: 차례는 그대로 p2, p1 은 순서 맨 뒤
        assertThat(game.getCurrentTurnPlayer().memberId()).isEqualTo(P2.memberId());
        assertThat(game.getPlayerOrder())
                .extracting(GamePlayer::memberId)
                .containsExactly(P2.memberId(), P3.memberId(), P1.memberId());
    }
}
