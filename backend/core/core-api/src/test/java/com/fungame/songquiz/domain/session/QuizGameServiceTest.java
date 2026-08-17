package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.Quiz;
import com.fungame.songquiz.domain.quiz.QuizContent;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuizGameServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final GamePlayer P1 = GamePlayer.createNewPlayer(1L, "p1");
    private static final GamePlayer P2 = GamePlayer.createNewPlayer(2L, "p2");
    private static final GamePlayer P3 = GamePlayer.createNewPlayer(3L, "p3");
    private static final GamePlayer P4 = GamePlayer.createNewPlayer(4L, "p4");
    private static final RoomSettings SETTINGS =
            new RoomSettings(GameType.SONG, "방", 8, Category.KPOP, 3, 0, CSQuizDifficulty.EASY);

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
        GameSession session = mock(GameSession.class);
        given(sessionManager.getGameSession(ROOM_ID)).willReturn(session);
        given(session.getContent()).willReturn(mock(QuizContent.class));

        // when
        quizGameService.startRound(ROOM_ID);

        // then
        verify(gameRoomManager).touch(ROOM_ID);
        verify(session).startRound();
        verify(timer).startCountDown(eq(ROOM_ID), anyInt(), any());
    }

    @Test
    @DisplayName("게임 중 이탈자는 랭킹과 스킵 정족수에서 제외된다.")
    void handlePlayerLeave_removes_player_from_session() {
        // given
        SongQuiz quiz = new SongQuiz(List.of(mock(Song.class)), Category.KPOP);
        GameSession session = new GameSession(quiz, List.of(P1, P2, P3));
        session.startRound();
        given(sessionManager.getGameSession(ROOM_ID)).willReturn(session);

        // when
        quizGameService.handlePlayerLeave(ROOM_ID, P2.memberId());

        // then
        assertThat(session.getPlayerRanks())
                .extracting(PlayerScore::memberId)
                .containsExactlyInAnyOrder(P1.memberId(), P3.memberId());

        // 남은 2명 중 1명만 스킵해도 정족수(max(1, n-1) = 1)를 채운다
        assertThat(session.handleAction(GameAction.skipVote(P1.memberId())))
                .isEqualTo(ActionResult.SKIP_VOTE_SUCCESS);
    }

    @Test
    @DisplayName("이탈로 남은 표가 줄어든 정족수를 채우면 라운드가 그 자리에서 끝난다.")
    void handlePlayerLeave_ends_round_when_skip_threshold_becomes_reached() {
        // given: 3명 중 1명이 스킵을 눌렀지만 정족수(max(1, 3-1) = 2)에 한 표 모자란다
        GameSession session = startedSessionOf(P1, P2, P3);
        quizGameService.increaseSkipVote(ROOM_ID, P1.memberId());

        // when: 다른 1명이 나가 정족수가 1로 내려간다
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
        startedSessionOf(P1, P2, P3, P4);
        quizGameService.increaseSkipVote(ROOM_ID, P1.memberId());

        // when: 1명이 나가도 정족수는 2 라 한 표로는 모자라다
        quizGameService.handlePlayerLeave(ROOM_ID, P2.memberId());

        // then
        verify(publisher, never()).publishEvent(any(RoundEndEvent.class));
        verify(timer, never()).stop(ROOM_ID);
    }

    @Test
    @DisplayName("세션이 이미 정리된 방의 이탈은 무시한다.")
    void handlePlayerLeave_ignores_missing_session() {
        // given
        given(sessionManager.getGameSession(ROOM_ID)).willReturn(null);

        // when & then: 예외 없이 통과
        quizGameService.handlePlayerLeave(ROOM_ID, P1.memberId());
    }

    @Test
    @DisplayName("첫 라운드가 시작되기 전에 들어온 채팅은 예외 없이 무시된다.")
    void processAnswer_before_first_round_is_ignored() {
        // given
        SongQuiz quiz = new SongQuiz(Stream.of(mock(Song.class)).toList(), Category.KPOP);
        GameSession session = new GameSession(quiz, List.of(P1));
        given(sessionManager.getGameSession(ROOM_ID)).willReturn(session);

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
        given(sessionManager.getGameSession(ROOM_ID)).willReturn(session);

        // when & then: 예외 없이 통과
        quizGameService.increaseSkipVote(ROOM_ID, P1.memberId());

        verify(publisher, never()).publishEvent(any(RoundEndEvent.class));
        verify(timer, never()).stop(ROOM_ID);
    }

    @Test
    @DisplayName("세션이 이미 정리된 방은 라운드를 시작하지 않는다.")
    void startRound_skips_when_session_gone() {
        // given
        given(sessionManager.getGameSession(ROOM_ID)).willReturn(null);

        // when
        quizGameService.startRound(ROOM_ID);

        // then
        verify(gameRoomManager, never()).touch(ROOM_ID);
        verify(timer, never()).startCountDown(eq(ROOM_ID), anyInt(), any());
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

        verify(gameRoomManager, never()).startGame(any(), any());
        verify(sessionManager, never()).startGame(any(), any(Quiz.class), any());
        verify(timer, never()).startAfter(any(), anyInt(), any());
    }

    @Test
    @DisplayName("문제가 있으면 방을 시작하고 첫 라운드를 예약한다.")
    void startGame_starts_when_quiz_has_round() {
        // given
        GameRoom startableRoom = givenStartableRoomWith(new SongQuiz(List.of(mock(Song.class)), Category.KPOP));
        List<GamePlayer> players = List.of(P1);

        given(gameRoomManager.startGame(ROOM_ID, P1.memberId())).willReturn(startableRoom);
        given(startableRoom.getRoomPlayers()).willReturn(players);
        given(sessionManager.startGame(eq(ROOM_ID), any(Quiz.class), eq(players)))
                .willReturn(new GameSession(new SongQuiz(List.of(mock(Song.class)), Category.KPOP), players));

        // when
        quizGameService.startGame(ROOM_ID, P1.memberId());

        // then
        verify(publisher).publishEvent(any(GameStartEvent.class));
        verify(timer).startAfter(eq(ROOM_ID), anyInt(), any());
    }

    private GameSession startedSessionOf(GamePlayer... players) {
        SongQuiz quiz = new SongQuiz(List.of(mock(Song.class), mock(Song.class)), Category.KPOP);
        GameSession session = new GameSession(quiz, List.of(players));
        session.startRound();
        given(sessionManager.getGameSession(ROOM_ID)).willReturn(session);

        return session;
    }

    private GameRoom givenStartableRoomWith(Quiz quiz) {
        GameRoom startableRoom = mock(GameRoom.class);

        given(gameRoomManager.findStartableRoom(ROOM_ID, P1.memberId())).willReturn(startableRoom);
        given(startableRoom.getSettings()).willReturn(SETTINGS);
        given(sessionManager.createQuiz(SETTINGS)).willReturn(quiz);

        return startableRoom;
    }
}
