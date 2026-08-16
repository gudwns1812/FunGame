package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.Quiz;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.enums.ActionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class GameSessionTest {

    GameSession gameSession;
    Quiz quiz;

    @BeforeEach
    void setUp() {
        quiz = mock(Quiz.class);
        List<GamePlayer> players = List.of(
                GamePlayer.createNewPlayer(1L, "p1"),
                GamePlayer.createNewPlayer(2L, "p2"),
                GamePlayer.createNewPlayer(3L, "p3"));
        gameSession = new GameSession(quiz, players);
    }

    @Test
    @DisplayName("정답 제출은 quiz 의 submitAnswer 로 위임된다.")
    void submitAnswer_delegates_to_game() {
        // given
        GameAction action = GameAction.submitAnswer(1L, "answer");
        given(quiz.submitAnswer(1L, "answer")).willReturn(ActionResult.CORRECT);

        // when
        ActionResult result = gameSession.handleAction(action);

        // then
        assertThat(result).isEqualTo(ActionResult.CORRECT);
        verify(quiz).submitAnswer(1L, "answer");
    }

    @Test
    @DisplayName("스킵 투표는 quiz 에 묻지 않고 세션이 명단을 보고 판단한다.")
    void skipVoteIsDecidedBySession() {
        // given: 세 명 중 두 명이 투표해야 스킵된다
        given(quiz.isRoundStarted()).willReturn(true);
        assertThat(gameSession.handleAction(GameAction.skipVote(1L))).isEqualTo(ActionResult.ACTION_SUCCESS);

        // when
        ActionResult result = gameSession.handleAction(GameAction.skipVote(2L));

        // then
        assertThat(result).isEqualTo(ActionResult.SKIP_VOTE_SUCCESS);
        verify(quiz, never()).submitAnswer(any(), any());
    }

    @Test
    @DisplayName("게임에 없는 사람의 스킵 투표는 세지 않는다.")
    void skipVoteFromStrangerIsIgnored() {
        given(quiz.isRoundStarted()).willReturn(true);

        assertThat(gameSession.handleAction(GameAction.skipVote(99L))).isEqualTo(ActionResult.NO_ACTION);
    }

    @Test
    @DisplayName("첫 라운드가 시작되기 전의 스킵 투표는 세지 않는다.")
    void skipVoteBeforeRoundStartIsIgnored() {
        given(quiz.isRoundStarted()).willReturn(false);

        assertThat(gameSession.handleAction(GameAction.skipVote(1L))).isEqualTo(ActionResult.NO_ACTION);
    }

    @Test
    @DisplayName("startProcessing 호출 시 game의 startProcessing으로 위임된다.")
    void startProcessing_delegates_to_game() {
        // given
        given(quiz.startProcessing()).willReturn(true);

        // when
        boolean result = gameSession.startProcessing();

        // then
        assertThat(result).isTrue();
        verify(quiz).startProcessing();
    }
}
