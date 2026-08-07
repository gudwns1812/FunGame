package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.event.GameResultEvent;
import com.fungame.songquiz.domain.event.HaliGaliActionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BoardGameServiceTest {

    private static final Long ROOM_ID = 1L;

    @Mock
    private GameRoomManager roomManager;
    @Mock
    private GameSessionManager sessionManager;
    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private BoardGameService boardGameService;

    @Test
    @DisplayName("게임 중 이탈자가 생기면 턴 순서에서 제거하고 갱신된 상태를 전파한다.")
    void handlePlayerLeave_removes_player_and_broadcasts() {
        // given
        HaliGaliGame game = new HaliGaliGame();
        game.setPlayers(List.of("p1", "p2", "p3"));
        GameSession session = new GameSession(game, List.of("p1", "p2", "p3"));
        given(sessionManager.getGameSession(ROOM_ID)).willReturn(session);

        String currentPlayer = game.getCurrentPlayer();

        // when
        boardGameService.handlePlayerLeave(ROOM_ID, currentPlayer);

        // then: 이탈자에게 턴이 걸려 멈추지 않는다
        assertThat(game.getCurrentPlayer()).isNotEqualTo(currentPlayer);
        assertThat(session.getPlayerRanks())
                .extracting(PlayerScore::player)
                .doesNotContain(currentPlayer);
        verify(publisher).publishEvent(any(HaliGaliActionEvent.class));
        verify(publisher, never()).publishEvent(any(GameResultEvent.class));
    }

    @Test
    @DisplayName("이탈로 남은 인원이 1명이 되면 게임을 종료하고 결과를 발행한다.")
    void handlePlayerLeave_ends_game_when_one_left() {
        // given
        HaliGaliGame game = new HaliGaliGame();
        game.setPlayers(List.of("p1", "p2"));
        GameSession session = new GameSession(game, List.of("p1", "p2"));
        given(sessionManager.getGameSession(ROOM_ID)).willReturn(session);

        // when
        boardGameService.handlePlayerLeave(ROOM_ID, "p2");

        // then
        verify(publisher).publishEvent(any(GameResultEvent.class));
        verify(sessionManager).endGameSession(ROOM_ID);
        verify(roomManager).endGame(ROOM_ID);
    }

    @Test
    @DisplayName("세션이 이미 정리된 방의 이탈은 무시한다.")
    void handlePlayerLeave_ignores_missing_session() {
        // given
        given(sessionManager.getGameSession(ROOM_ID)).willReturn(null);

        // when
        boardGameService.handlePlayerLeave(ROOM_ID, "p1");

        // then
        verify(publisher, never()).publishEvent(any());
        verify(roomManager, never()).endGame(ROOM_ID);
    }
}
