package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.Quiz;
import com.fungame.songquiz.domain.quiz.Song;
import com.fungame.songquiz.domain.quiz.SongQuiz;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.GameRoom;
import com.fungame.songquiz.domain.room.GameRoomManager;
import com.fungame.songquiz.domain.room.RoomSettings;
import com.fungame.songquiz.enums.ActionResult;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuizGameServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final GamePlayer P1 = GamePlayer.createNewPlayer(1L);
    private static final GamePlayer P2 = GamePlayer.createNewPlayer(2L);
    private static final GamePlayer P3 = GamePlayer.createNewPlayer(3L);
    private static final GamePlayer P4 = GamePlayer.createNewPlayer(4L);
    private static final RoomSettings SETTINGS =
            new RoomSettings(GameType.SONG, "방", 8, Category.KPOP, 3, 0, CSQuizDifficulty.EASY);
    private static final Duration ROUND_LENGTH = Duration.ofSeconds(30);
    private static final Duration UNTIL_HINT_OPENS = Duration.ofSeconds(20);
    private static final Duration BEFORE_GAME_RESULT = Duration.ofSeconds(3);
    private static final Duration BETWEEN_ROUNDS = Duration.ofSeconds(3);
    private static final long TICK_TOLERANCE_MILLIS = 500L;

    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private GameRoomManager gameRoomManager;
    @Mock
    private GameSessionManager sessionManager;
    @Mock
    private GameTimer timer;

    @InjectMocks
    private QuizGameService quizGameService;

    @Test
    @DisplayName("라운드가 시작되면 방의 활동 시각을 갱신해 유휴 청소 대상에서 벗어난다.")
    void startRound_touches_room() {
        // given
        GameSession session = startedSessionOf(P1);

        // when
        quizGameService.startRound(ROOM_ID);

        // then
        verify(gameRoomManager).touch(ROOM_ID);
        assertThat(session.getCurrentRound()).isEqualTo(2);
    }

    @Test
    @DisplayName("라운드가 시작되면 힌트와 타임아웃을 각각 20초·30초 뒤로 예약한다.")
    void startRound_schedules_hint_and_timeout_separately() {
        // given
        startedSessionOf(P1);

        // when
        quizGameService.startRound(ROOM_ID);

        // then
        verify(timer).startAfter(eq(UNTIL_HINT_OPENS), any());
        verify(timer).startAfter(eq(ROUND_LENGTH), any());
    }

    @Test
    @DisplayName("라운드 시작 알림에 남은 시간이 실려 클라이언트가 그 시점부터 센다.")
    void startRound_tells_how_long_is_left() {
        // given
        startedSessionOf(P1);

        // when
        quizGameService.startRound(ROOM_ID);

        // then
        ArgumentCaptor<RoundStartEvent> roundStart = ArgumentCaptor.forClass(RoundStartEvent.class);
        verify(publisher).publishEvent(roundStart.capture());
        assertThat(roundStart.getValue().remainingMillis()).isEqualTo(ROUND_LENGTH.toMillis());
    }

    @Test
    @DisplayName("라운드가 조기 종료되면 그 방의 예약을 모두 취소해 남은 힌트 예약도 흘려보낸다.")
    void endRound_cancels_every_reservation_of_the_room() {
        // given
        GameSession session = startedSessionOf(P1);
        quizGameService.startRound(ROOM_ID);

        // when
        quizGameService.increaseSkipVote(ROOM_ID, P1.memberId());

        // then
        verify(timer).stop(ROOM_ID);
        assertThat(session.getRemainingRoundMillis()).isZero();
    }

    @Test
    @DisplayName("라운드 도중에 들어온 사람도 스냅샷으로 남은 시간을 알 수 있다.")
    void getPlayState_carries_remaining_round_time() {
        // given
        startedSessionOf(P1);

        // when
        GameStateDto state = quizGameService.getPlayState(ROOM_ID);

        // then
        assertThat(state.remainingMillis())
                .isBetween(ROUND_LENGTH.toMillis() - TICK_TOLERANCE_MILLIS, ROUND_LENGTH.toMillis());
    }

    @Test
    @DisplayName("라운드가 닫힌 뒤의 스냅샷은 남은 시간이 없다.")
    void getPlayState_reports_no_time_left_between_rounds() {
        // given
        GameSession session = startedSessionOf(P1);
        session.startProcessing();

        // when
        GameStateDto state = quizGameService.getPlayState(ROOM_ID);

        // then
        assertThat(state.remainingMillis()).isZero();
    }

    @Test
    @DisplayName("이탈로 남은 표가 줄어든 정족수를 채우면 라운드가 그 자리에서 끝난다.")
    void handlePlayerLeave_ends_round_when_skip_threshold_becomes_reached() {
        // given: 3명 중 1명이 스킵을 눌렀지만 정족수(max(1, 3-1) = 2)에 한 표 모자란다
        GameSession session = startedSessionOf(P1, P2, P3);
        quizGameService.increaseSkipVote(ROOM_ID, P1.memberId());

        // when: 다른 1명이 나가 정족수가 1로 내려간다
        session.removePlayer(P2.memberId());
        quizGameService.handlePlayerLeave(ROOM_ID, P2.memberId());

        // then
        verify(publisher).publishEvent(any(RoundEndEvent.class));
        verify(timer).stop(ROOM_ID);
        assertThat(session.getPlayerRanks()).hasSize(2);
    }

    @Test
    @DisplayName("이탈해도 정족수에 모자라면 라운드를 그대로 둔다.")
    void handlePlayerLeave_keeps_round_when_skip_threshold_still_short() {
        // given: 4명 중 1명이 스킵을 눌렀고 정족수는 max(1, 4-1) = 3 이다
        GameSession session = startedSessionOf(P1, P2, P3, P4);
        quizGameService.increaseSkipVote(ROOM_ID, P1.memberId());

        // when: 1명이 나가도 정족수는 2 라 한 표로는 모자라다
        session.removePlayer(P2.memberId());
        quizGameService.handlePlayerLeave(ROOM_ID, P2.memberId());

        // then
        verify(publisher, never()).publishEvent(any(RoundEndEvent.class));
        verify(timer, never()).stop(ROOM_ID);
    }

    @Test
    @DisplayName("세션이 이미 정리된 방의 이탈은 무시한다.")
    void handlePlayerLeave_ignores_missing_session() {
        // given
        noSession();

        // when & then: 예외 없이 통과
        quizGameService.handlePlayerLeave(ROOM_ID, P1.memberId());
    }

    @Test
    @DisplayName("첫 라운드가 시작되기 전에 들어온 채팅은 예외 없이 무시된다.")
    void processAnswer_before_first_round_is_ignored() {
        // given
        SongQuiz quiz = new SongQuiz(Stream.of(mock(Song.class)).toList(), Category.KPOP);
        GameSession session = new GameSession(quiz, List.of(P1));
        useSession(session);

        // when & then: 예외 없이 통과
        quizGameService.processAnswer(ROOM_ID, P1.memberId(), "아무 채팅");

        verify(publisher, never()).publishEvent(any(RoundEndEvent.class));
    }

    @Test
    @DisplayName("첫 라운드가 시작되기 전에 들어온 스킵 투표는 무시되고 첫 라운드 예약을 취소하지 않는다.")
    void increaseSkipVote_before_first_round_is_ignored() {
        // given
        SongQuiz quiz = new SongQuiz(Stream.of(mock(Song.class)).toList(), Category.KPOP);
        GameSession session = new GameSession(quiz, List.of(P1));
        useSession(session);

        // when & then: 예외 없이 통과
        quizGameService.increaseSkipVote(ROOM_ID, P1.memberId());

        verify(publisher, never()).publishEvent(any(RoundEndEvent.class));
        verify(timer, never()).stop(ROOM_ID);
    }

    @Test
    @DisplayName("세션이 이미 정리된 방은 라운드를 시작하지 않는다.")
    void startRound_skips_when_session_gone() {
        // given
        noSession();

        // when
        quizGameService.startRound(ROOM_ID);

        // then
        verify(gameRoomManager, never()).touch(ROOM_ID);
        verify(timer, never()).startAfter(any(Duration.class), any());
    }

    @Test
    @DisplayName("출제할 문제가 없으면 방을 시작하지 않고 거절한다.")
    void startGame_rejected_when_quiz_has_no_round() {
        // given
        givenStartableRoomWith(new SongQuiz(List.of(), Category.KPOP));

        // when & then
        assertThatThrownBy(() -> quizGameService.startGame(ROOM_ID, P1.memberId()))
                .isInstanceOf(CoreException.class)
                .extracting(thrown -> ((CoreException) thrown).getType())
                .isEqualTo(ErrorType.QUIZ_EMPTY);

        verify(gameRoomManager, never()).startGame(any(), any(), any());
        verify(timer, never()).startAfter(any(), any());
    }

    @Test
    @DisplayName("문제가 있으면 방을 시작하고 첫 라운드를 예약한다.")
    void startGame_starts_when_quiz_has_round() {
        // given
        givenStartableRoomWith(new SongQuiz(List.of(mock(Song.class)), Category.KPOP));
        given(gameRoomManager.startGame(eq(ROOM_ID), eq(P1.memberId()), any()))
                .willReturn(new GameSession(new SongQuiz(List.of(mock(Song.class)), Category.KPOP), List.of(P1)));

        // when
        quizGameService.startGame(ROOM_ID, P1.memberId());

        // then
        verify(publisher).publishEvent(any(GameStartEvent.class));
        verify(timer).startAfter(any(Duration.class), eq(GameTimerTask.startRound(ROOM_ID, 1)));
    }

    @Test
    @DisplayName("결과 브로드캐스트가 터져도 방은 정리된다. PLAYING 으로 굳지 않는다.")
    void endGame_tears_down_the_room_even_if_the_result_broadcast_blows_up() {
        // given: 문제가 하나뿐이라 이번 라운드가 마지막이다
        lastRoundSessionOf(P1);
        quizGameService.increaseSkipVote(ROOM_ID, P1.memberId());

        verify(timer).startAfter(BEFORE_GAME_RESULT, GameTimerTask.showResult(ROOM_ID));

        willThrow(new IllegalStateException("브로드캐스트 실패"))
                .given(publisher).publishEvent(any(GameResultEvent.class));

        // when
        assertThatThrownBy(() -> quizGameService.onTimer(GameTimerTask.showResult(ROOM_ID)))
                .isInstanceOf(IllegalStateException.class);

        // then
        verify(gameRoomManager).endGame(ROOM_ID);
    }

    @Test
    @DisplayName("라운드 시작 알림이 터져도 타임아웃은 예약된다. 방이 그 라운드에서 굳지 않는다.")
    void startRound_schedules_timeout_even_if_the_start_broadcast_blows_up() {
        // given: 라운드 시작 알림에는 @Async 가 없어 리스너의 예외가 여기까지 올라온다
        startedSessionOf(P1);
        willThrow(new IllegalStateException("브로드캐스트 실패"))
                .given(publisher).publishEvent(any(RoundStartEvent.class));

        // when: 예외는 그대로 올려 GameTimer 가 어느 방인지 남기게 한다
        assertThatThrownBy(() -> quizGameService.startRound(ROOM_ID))
                .isInstanceOf(IllegalStateException.class);

        // then: 라운드를 연 이상 닫을 사람도 있어야 한다
        verify(timer).startAfter(eq(ROUND_LENGTH), any());
    }

    @Test
    @DisplayName("라운드 결과 브로드캐스트가 터져도 다음 라운드는 예약된다.")
    void endRound_schedules_next_round_even_if_the_result_broadcast_blows_up() {
        // given: 문제가 둘이라 이번 라운드 뒤에 다음 라운드가 남아 있다
        startedSessionOf(P1);
        willThrow(new IllegalStateException("브로드캐스트 실패"))
                .given(publisher).publishEvent(any(RoundEndEvent.class));

        // when: 스킵 정족수를 채워 라운드를 끝낸다
        assertThatThrownBy(() -> quizGameService.increaseSkipVote(ROOM_ID, P1.memberId()))
                .isInstanceOf(IllegalStateException.class);

        // then: startProcessing 은 되돌릴 수 없다. 여기서 예약을 놓치면 그 방은 영영 멈춘다
        verify(timer).startAfter(eq(BETWEEN_ROUNDS), any());
    }

    @Test
    @DisplayName("마지막 라운드의 결과 브로드캐스트가 터져도 게임 종료 단계는 예약된다.")
    void endRound_schedules_game_over_even_if_the_last_result_broadcast_blows_up() {
        // given: 문제가 하나뿐이라 이번 라운드가 마지막이다
        lastRoundSessionOf(P1);
        willThrow(new IllegalStateException("브로드캐스트 실패"))
                .given(publisher).publishEvent(any(RoundEndEvent.class));

        // when
        assertThatThrownBy(() -> quizGameService.increaseSkipVote(ROOM_ID, P1.memberId()))
                .isInstanceOf(IllegalStateException.class);

        // then: 이 예약이 방 정리까지 이어진다
        verify(timer).startAfter(eq(BEFORE_GAME_RESULT), any());
    }

    @Test
    @DisplayName("같은 라운드 시작 예약이 두 번 돌아도 라운드는 한 번만 넘어간다. 리스가 끝나 다른 서버가 다시 돌려도 안전하다.")
    void startRound_task_running_twice_advances_once() {
        GameSession session = startedSessionOf(P1);

        quizGameService.onTimer(GameTimerTask.startRound(ROOM_ID, 2));
        quizGameService.onTimer(GameTimerTask.startRound(ROOM_ID, 2));

        assertThat(session.getCurrentRound()).isEqualTo(2);
        verify(publisher).publishEvent(any(RoundStartEvent.class));
    }

    @Test
    @DisplayName("지난 라운드의 시간 초과 예약은 지금 라운드를 끝내지 않는다.")
    void stale_timeout_does_not_end_the_current_round() {
        startedSessionOf(P1);

        quizGameService.onTimer(GameTimerTask.endRound(ROOM_ID, 0));

        verify(publisher, never()).publishEvent(any(RoundEndEvent.class));
    }

    @Test
    @DisplayName("시간 초과 예약이 두 번 돌아도 라운드는 한 번만 끝난다.")
    void timeout_task_running_twice_ends_once() {
        startedSessionOf(P1);

        quizGameService.onTimer(GameTimerTask.endRound(ROOM_ID, 1));
        quizGameService.onTimer(GameTimerTask.endRound(ROOM_ID, 1));

        verify(publisher).publishEvent(any(RoundEndEvent.class));
    }

    @Test
    @DisplayName("판이 이미 끝난 방의 결과 예약은 아무것도 하지 않는다. 결과가 두 번 나가지 않는다.")
    void show_result_without_game_does_nothing() {
        noSession();

        quizGameService.onTimer(GameTimerTask.showResult(ROOM_ID));

        verify(publisher, never()).publishEvent(any(GameResultEvent.class));
        verify(gameRoomManager, never()).endGame(ROOM_ID);
    }

    @Test
    @DisplayName("지난 라운드의 힌트 예약은 힌트를 내지 않는다.")
    void stale_hint_is_not_opened() {
        startedSessionOf(P1);

        quizGameService.onTimer(GameTimerTask.openHint(ROOM_ID, 0));

        verify(publisher, never()).publishEvent(any(QuizGameHintEvent.class));
    }

    private void useSession(GameSession session) {
        lenient().when(sessionManager.find(ROOM_ID)).thenReturn(Optional.of(session));
        lenient().when(sessionManager.update(eq(ROOM_ID), any())).thenAnswer(invocation -> {
            Function<GameSession, ?> change = invocation.getArgument(1);
            return Optional.ofNullable(change.apply(session));
        });
    }

    private void noSession() {
        lenient().when(sessionManager.find(ROOM_ID)).thenReturn(Optional.empty());
        lenient().when(sessionManager.update(eq(ROOM_ID), any())).thenReturn(Optional.empty());
    }

    private GameSession startedSessionOf(GamePlayer... players) {
        SongQuiz quiz = new SongQuiz(List.of(playableSong(), playableSong()), Category.KPOP);
        GameSession session = new GameSession(quiz, List.of(players));
        session.startRound();
        useSession(session);

        return session;
    }

    private void lastRoundSessionOf(GamePlayer... players) {
        SongQuiz quiz = new SongQuiz(List.of(playableSong()), Category.KPOP);
        GameSession session = new GameSession(quiz, List.of(players));
        session.startRound();
        useSession(session);
    }

    private static Song playableSong() {
        Song song = mock(Song.class);
        lenient().when(song.getLink()).thenReturn("youtube-link");
        lenient().when(song.getSinger()).thenReturn("가수");
        lenient().when(song.getHint()).thenReturn("ㅎㅌ");

        return song;
    }

    private GameRoom givenStartableRoomWith(Quiz quiz) {
        GameRoom startableRoom = mock(GameRoom.class);

        given(gameRoomManager.findStartableRoom(ROOM_ID, P1.memberId())).willReturn(startableRoom);
        given(startableRoom.getSettings()).willReturn(SETTINGS);
        given(sessionManager.createQuiz(SETTINGS)).willReturn(quiz);

        return startableRoom;
    }
}
