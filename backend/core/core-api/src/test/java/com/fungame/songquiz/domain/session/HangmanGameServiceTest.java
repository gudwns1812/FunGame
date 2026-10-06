package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.HangmanQuiz;
import com.fungame.songquiz.domain.quiz.HangmanWord;
import com.fungame.songquiz.domain.quiz.Quiz;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.GameRoom;
import com.fungame.songquiz.domain.room.GameRoomManager;
import com.fungame.songquiz.domain.room.RoomSettings;
import com.fungame.songquiz.enums.ActionType;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HangmanGameServiceTest {

    private static final GamePlayer HOST = GamePlayer.createNewPlayer(1L);
    private static final GamePlayer P1 = GamePlayer.createNewPlayer(1L);
    private static final GamePlayer P2 = GamePlayer.createNewPlayer(2L);
    private static final RoomSettings SETTINGS =
            new RoomSettings(GameType.HANGMAN, "방", 8, Category.DEFAULT, 1, 0, CSQuizDifficulty.EASY);

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
        List<GamePlayer> players = List.of(HOST, P2);
        GameRoom startableRoom = mock(GameRoom.class);
        HangmanQuiz preparedQuiz = HangmanQuiz.create(new HangmanWord(1L, "APPLE", 1));
        given(gameRoomManager.findStartableRoom(roomId, HOST.memberId())).willReturn(startableRoom);
        given(startableRoom.getSettings()).willReturn(SETTINGS);
        given(startableRoom.getRoomPlayers()).willReturn(players);
        given(gameSessionManager.createQuiz(SETTINGS)).willReturn(preparedQuiz);
        given(gameRoomManager.startGame(eq(roomId), eq(HOST.memberId()), any())).willAnswer(invocation -> {
            Function<GameRoom, Quiz> quizFor = invocation.getArgument(2);
            return new GameSession(quizFor.apply(startableRoom), players);
        });

        // When
        hangmanGameService.startGame(roomId, HOST.memberId());

        // Then
        verify(eventPublisher).publishEvent(any(GameStartEvent.class));
    }

    @Test
    @DisplayName("플레이어 액션 시 HangmanActionEvent를 발행한다.")
    void handleAction_success() {
        // Given
        Long roomId = 1L;
        List<GamePlayer> players = List.of(P1, P2);
        HangmanQuiz hangmanQuiz = HangmanQuiz.create(new HangmanWord(1L, "APPLE", 1));
        hangmanQuiz.initPlayers(players);
        GameAction action = new GameAction(P1.memberId(), ActionType.SUBMIT_ANSWER, "A");

        useSession(roomId, new GameSession(hangmanQuiz, players));

        // When
        hangmanGameService.handleAction(roomId, action);

        // Then
        verify(eventPublisher).publishEvent(any(HangmanActionEvent.class));
    }

    @Test
    @DisplayName("게임이 승리 상태로 종료되면 결과를 발행하고 방을 대기 상태로 되돌린다.")
    void handleAction_win_ends_room() {
        // Given
        Long roomId = 1L;
        List<GamePlayer> players = List.of(P1);
        HangmanQuiz hangmanQuiz = HangmanQuiz.create(new HangmanWord(2L, "A", 1)); // 한 글자 정답
        hangmanQuiz.initPlayers(players);
        GameAction action = new GameAction(P1.memberId(), ActionType.SUBMIT_ANSWER, "A");

        useSession(roomId, new GameSession(hangmanQuiz, players));

        // When
        hangmanGameService.handleAction(roomId, action);

        // Then
        verify(gameRoomManager).endGame(roomId);
        verify(eventPublisher).publishEvent(any(HangmanActionEvent.class));
        verify(eventPublisher).publishEvent(any(GameResultEvent.class));
    }

    @Test
    @DisplayName("결과 브로드캐스트가 터져도 방은 정리된다. PLAYING 으로 굳지 않는다.")
    void ends_the_room_even_if_the_result_broadcast_blows_up() {
        // Given
        Long roomId = 1L;
        List<GamePlayer> players = List.of(P1);
        HangmanQuiz hangmanQuiz = HangmanQuiz.create(new HangmanWord(2L, "A", 1)); // 한 글자 정답
        hangmanQuiz.initPlayers(players);
        GameAction action = new GameAction(P1.memberId(), ActionType.SUBMIT_ANSWER, "A");

        useSession(roomId, new GameSession(hangmanQuiz, players));
        // 이 방의 다른 이벤트 발행까지 막지 않도록 lenient 로 둔다
        lenient().doThrow(new IllegalStateException("브로드캐스트 실패"))
                .when(eventPublisher).publishEvent(any(GameResultEvent.class));

        // When
        assertThatThrownBy(() -> hangmanGameService.handleAction(roomId, action))
                .isInstanceOf(IllegalStateException.class);

        // Then
        verify(gameRoomManager).endGame(roomId);
    }

    private void useSession(Long roomId, GameSession session) {
        lenient().when(gameSessionManager.find(roomId)).thenReturn(Optional.of(session));
        lenient().when(gameSessionManager.update(eq(roomId), any())).thenAnswer(invocation -> {
            Function<GameSession, ?> change = invocation.getArgument(1);
            return Optional.ofNullable(change.apply(session));
        });
    }
}
