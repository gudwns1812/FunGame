package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameContentDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
        given(session.getContent()).willReturn(mock(GameContentDto.class));

        // when
        quizGameService.startRound(ROOM_ID);

        // then
        verify(gameRoomManager).touch(ROOM_ID);
        verify(session).startRound();
        verify(timer).startCountDown(eq(ROOM_ID), anyInt(), any());
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
