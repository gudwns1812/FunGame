package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.QuizContent;
import com.fungame.songquiz.domain.quiz.Song;
import com.fungame.songquiz.domain.quiz.SongQuiz;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.GameRoomManager;
import com.fungame.songquiz.enums.ActionResult;
import com.fungame.songquiz.enums.Category;
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
}
