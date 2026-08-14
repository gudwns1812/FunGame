package com.fungame.songquiz.domain;

import com.fungame.songquiz.enums.ActionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
    @DisplayName("handleAction 호출 시 game의 handleAction으로 위임된다.")
    void handleAction_delegates_to_game() {
        // given
        GameAction action = GameAction.submitAnswer(1L, "answer");
        given(game.handleAction(action)).willReturn(ActionResult.CORRECT);

        // when
        ActionResult result = gameSession.handleAction(action);

        // then
        assertThat(result).isEqualTo(ActionResult.CORRECT);
        verify(game).handleAction(action);
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
