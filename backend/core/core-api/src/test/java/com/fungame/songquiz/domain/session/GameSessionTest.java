package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.Game;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.enums.ActionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class GameSessionTest {

    GameSession gameSession;
    Game game;

    @BeforeEach
    void setUp() {
        game = mock(Game.class);
        List<GamePlayer> players = List.of(
                GamePlayer.createNewPlayer(1L, "p1"),
                GamePlayer.createNewPlayer(2L, "p2"),
                GamePlayer.createNewPlayer(3L, "p3"));
        gameSession = new GameSession(game, players);
    }

    @Test
    @DisplayName("정답 제출은 game 의 submitAnswer 로 위임된다.")
    void submitAnswer_delegates_to_game() {
        // given
        GameAction action = GameAction.submitAnswer(1L, "answer");
        given(game.submitAnswer(1L, "answer")).willReturn(ActionResult.CORRECT);

        // when
        ActionResult result = gameSession.handleAction(action);

        // then
        assertThat(result).isEqualTo(ActionResult.CORRECT);
        verify(game).submitAnswer(1L, "answer");
    }

    @Test
    @DisplayName("스킵 투표는 game 에 묻지 않고 세션이 명단을 보고 판단한다.")
    void skipVoteIsDecidedBySession() {
        // given: 세 명 중 두 명이 투표해야 스킵된다
        assertThat(gameSession.handleAction(GameAction.skipVote(1L))).isEqualTo(ActionResult.ACTION_SUCCESS);

        // when
        ActionResult result = gameSession.handleAction(GameAction.skipVote(2L));

        // then
        assertThat(result).isEqualTo(ActionResult.SKIP_VOTE_SUCCESS);
        verifyNoInteractions(game);
    }

    @Test
    @DisplayName("게임에 없는 사람의 스킵 투표는 세지 않는다.")
    void skipVoteFromStrangerIsIgnored() {
        assertThat(gameSession.handleAction(GameAction.skipVote(99L))).isEqualTo(ActionResult.NO_ACTION);
    }

    @Test
    @DisplayName("startProcessing 호출 시 game의 startProcessing으로 위임된다.")
    void startProcessing_delegates_to_game() {
        // given
        given(game.startProcessing()).willReturn(true);

        // when
        boolean result = gameSession.startProcessing();

        // then
        assertThat(result).isTrue();
        verify(game).startProcessing();
    }
}
