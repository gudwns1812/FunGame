package com.fungame.songquiz.domain;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.lock.LockContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameRoomManagerTest {

    private static final Long ROOM_ID = 1L;
    private static final String HOST = "방장";

    @Mock
    ApplicationEventPublisher applicationEventPublisher;

    @Mock
    GameTimer gameTimer;

    @Mock
    GameSessionManager gameSessionManager;

    GameRoomManager gameRoomManager;

    @BeforeEach
    void setUp() {
        gameRoomManager = new GameRoomManager(
                new LockContext(),
                applicationEventPublisher,
                gameTimer,
                gameSessionManager
        );
    }

    @Test
    void 마지막_플레이어가_나가면_방과_함께_타이머와_게임세션도_정리한다() {
        // given
        gameRoomManager.createGameRoom(ROOM_ID, "방", mock(Game.class), HOST, 8);
        gameRoomManager.startGame(ROOM_ID, HOST);

        // when
        GameRoomManager.LeaveResult result = gameRoomManager.leaveRoom(ROOM_ID, HOST);

        // then
        assertThat(result.destroyed()).isTrue();
        assertThat(result.wasPlaying()).isTrue();
        verify(gameTimer).stop(ROOM_ID);
        verify(gameSessionManager).endGameSession(ROOM_ID);
        assertThatThrownBy(() -> gameRoomManager.findRoom(ROOM_ID))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void 남은_플레이어가_있으면_게임_상태를_정리하지_않는다() {
        // given
        gameRoomManager.createGameRoom(ROOM_ID, "방", mock(Game.class), HOST, 8);
        gameRoomManager.joinRoom(ROOM_ID, "참가자");

        // when
        GameRoomManager.LeaveResult result = gameRoomManager.leaveRoom(ROOM_ID, HOST);

        // then
        assertThat(result.destroyed()).isFalse();
        assertThat(result.wasPlaying()).isFalse();
        verify(gameTimer, never()).stop(ROOM_ID);
        verify(gameSessionManager, never()).endGameSession(ROOM_ID);
    }

    @Test
    void 오래_방치된_방은_게임_상태와_함께_정리된다() {
        // given
        gameRoomManager.createGameRoom(ROOM_ID, "방", mock(Game.class), HOST, 8);
        makeIdle(ROOM_ID);

        // when
        gameRoomManager.cleanupIdleRooms();

        // then
        verify(gameTimer).stop(ROOM_ID);
        verify(gameSessionManager).endGameSession(ROOM_ID);
        assertThatThrownBy(() -> gameRoomManager.findRoom(ROOM_ID))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void 이미_정리된_방을_다시_삭제해도_정리가_중복_수행되지_않는다() {
        // given
        gameRoomManager.createGameRoom(ROOM_ID, "방", mock(Game.class), HOST, 8);
        gameRoomManager.leaveRoom(ROOM_ID, HOST);

        // when: 게임 종료 타이머가 뒤늦게 endGame 을 호출하는 상황
        gameRoomManager.endGame(ROOM_ID);

        // then
        verify(gameTimer, times(1)).stop(ROOM_ID);
        verify(gameSessionManager, times(1)).endGameSession(ROOM_ID);
    }

    @Test
    void 없는_방을_touch하면_NPE가_아니라_CoreException이_발생한다() {
        assertThatThrownBy(() -> gameRoomManager.touch(ROOM_ID))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void touch한_방은_유휴_청소_대상에서_제외된다() {
        // given
        gameRoomManager.createGameRoom(ROOM_ID, "방", mock(Game.class), HOST, 8);
        makeIdle(ROOM_ID);

        // when
        gameRoomManager.touch(ROOM_ID);
        gameRoomManager.cleanupIdleRooms();

        // then
        assertThat(gameRoomManager.findRoom(ROOM_ID)).isNotNull();
        verify(gameTimer, never()).stop(ROOM_ID);
    }

    private void makeIdle(Long roomId) {
        GameRoom room = gameRoomManager.findRoom(roomId);
        ReflectionTestUtils.setField(room, "lastActivityTime", Instant.now().minus(31, ChronoUnit.MINUTES));
    }
}
