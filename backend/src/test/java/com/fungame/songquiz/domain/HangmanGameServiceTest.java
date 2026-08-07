package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameContentDto;
import com.fungame.songquiz.domain.dto.GameInfo;
import com.fungame.songquiz.domain.event.GameResultEvent;
import com.fungame.songquiz.domain.event.GameStartEvent;
import com.fungame.songquiz.domain.event.HangmanActionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HangmanGameServiceTest {

    @Mock
    private GameRoomManager gameRoomManager;
    @Mock
    private GameSessionManager gameSessionManager;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private HangmanGameService hangmanGameService;

    @Test
    @DisplayName("게임 시작 시 GameStartEvent를 발행한다.")
    void startGame_success() {
        // Given
        Long roomId = 1L;
        String nickname = "host";
        List<String> players = List.of("host", "p2");
        GameRoom mockRoom = mock(GameRoom.class);
        HangmanGame mockGame = HangmanGame.create("APPLE");
        GameInfo mockGameInfo = new GameInfo("HANGMAN", "Hangman", 6);

        given(gameRoomManager.startGame(roomId, nickname)).willReturn(mockRoom);
        given(mockRoom.getGame()).willReturn(mockGame);
        given(mockRoom.getRoomPlayers()).willReturn(players);
        given(gameSessionManager.startGame(eq(roomId), eq(mockGame), eq(players))).willReturn(mockGameInfo);

        // When
        hangmanGameService.startGame(roomId, nickname);

        // Then
        verify(eventPublisher).publishEvent(any(GameStartEvent.class));
    }

    @Test
    @DisplayName("플레이어 액션 시 HangmanActionEvent를 발행한다.")
    void handleAction_success() {
        // Given
        Long roomId = 1L;
        List<String> players = List.of("p1", "p2");
        GameRoom mockRoom = mock(GameRoom.class);
        HangmanGame mockGame = HangmanGame.create("APPLE");
        mockGame.initPlayers(players);
        GameAction action = new GameAction("p1", ActionType.SUBMIT_ANSWER, "A");

        given(gameRoomManager.findRoom(roomId)).willReturn(mockRoom);
        given(mockRoom.getGame()).willReturn(mockGame);

        // When
        hangmanGameService.handleAction(roomId, action);

        // Then
        verify(eventPublisher).publishEvent(any(HangmanActionEvent.class));
    }

    @Test
    @DisplayName("게임이 승리 상태로 종료되면 방과 게임 세션을 정리하고 결과를 발행한다.")
    void handleAction_win_ends_room() {
        // Given
        Long roomId = 1L;
        List<String> players = List.of("p1");
        GameRoom mockRoom = mock(GameRoom.class);
        HangmanGame mockGame = HangmanGame.create("A"); // 한 글자 정답
        mockGame.initPlayers(players);
        GameAction action = new GameAction("p1", ActionType.SUBMIT_ANSWER, "A");

        given(gameRoomManager.findRoom(roomId)).willReturn(mockRoom);
        given(mockRoom.getGame()).willReturn(mockGame);

        // When
        hangmanGameService.handleAction(roomId, action);

        // Then
        verify(gameRoomManager).endGame(roomId);
        verify(gameSessionManager).endGameSession(roomId);
        verify(eventPublisher).publishEvent(any(HangmanActionEvent.class));
        verify(eventPublisher).publishEvent(any(GameResultEvent.class));
    }
}
