package com.fungame.songquiz.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GameSessionRejoinTest {

    private GameSession quizSession(String... players) {
        SongQuiz game = new SongQuiz(List.of(mock(Song.class)), Category.KPOP);
        return new GameSession(game, List.of(players));
    }

    @Test
    @DisplayName("이탈했던 참가자는 재입장할 수 있고, 이탈 전 점수를 그대로 이어받는다.")
    void rejoin_restores_score() {
        // given
        GameSession session = quizSession("p1", "p2");
        session.updatePlayerPoint("p1");
        session.updatePlayerPoint("p1");
        session.removePlayer("p1");

        // then: 이탈 중에는 순위에서 빠진다
        assertThat(session.getPlayerRanks()).extracting(PlayerScore::player).containsExactly("p2");
        assertThat(session.canRejoin("p1")).isTrue();

        // when
        session.restorePlayer("p1");

        // then: 점수가 보존된 채 복귀한다
        assertThat(session.getPlayerRanks())
                .containsExactly(new PlayerScore("p1", 2), new PlayerScore("p2", 0));
        assertThat(session.canRejoin("p1")).isFalse();
    }

    @Test
    @DisplayName("이 게임에 참가한 적 없는 사람은 재입장할 수 없다.")
    void non_participant_cannot_rejoin() {
        GameSession session = quizSession("p1", "p2");

        assertThat(session.canRejoin("난입자")).isFalse();
    }

    @Test
    @DisplayName("접속 중인 참가자는 재입장 대상이 아니다.")
    void active_player_is_not_rejoin_target() {
        GameSession session = quizSession("p1", "p2");

        assertThat(session.canRejoin("p1")).isFalse();
    }

    @Test
    @DisplayName("행맨 재입장자는 턴 순서 맨 뒤에 붙어 진행 중인 차례를 흐트러뜨리지 않는다.")
    void hangman_rejoin_appends_to_turn_order() {
        // given: p1, p2, p3 중 p2 차례
        HangmanGame game = HangmanGame.create("APPLE");
        game.initPlayers(List.of("p1", "p2", "p3"));
        GameSession session = new GameSession(game, List.of("p1", "p2", "p3"));
        game.guess("p1", 'A');
        assertThat(game.getCurrentTurnPlayer()).isEqualTo("p2");

        // when: p1 이 나갔다 돌아온다
        session.removePlayer("p1");
        assertThat(game.getCurrentTurnPlayer()).isEqualTo("p2");
        session.restorePlayer("p1");

        // then: 차례는 그대로 p2, p1 은 순서 맨 뒤
        assertThat(game.getCurrentTurnPlayer()).isEqualTo("p2");
        assertThat(game.getPlayerOrder()).containsExactly("p2", "p3", "p1");
    }
}
