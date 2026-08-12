package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.GameRoomStore;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameRoomManagerTest {

    private static final Long ROOM_ID = 1L;
    private static final String HOST = "방장";
    private static final RoomSettings SETTINGS = new RoomSettings(GameType.SONG, "방", 8, Category.KPOP, 10, 0);

    @Mock
    ApplicationEventPublisher applicationEventPublisher;

    @Mock
    GameTimer gameTimer;

    @Mock
    GameSessionManager gameSessionManager;

    @Mock
    GameRoomStore gameRoomStore;

    @Mock
    GameFactories gameFactories;

    GameRoomManager gameRoomManager;

    @BeforeEach
    void setUp() {
        gameRoomManager = new GameRoomManager(
                new LockContext(),
                applicationEventPublisher,
                gameTimer,
                gameSessionManager,
                gameRoomStore,
                gameFactories
        );
    }

    private void openRoom(int maxPlayers) {
        RoomSettings settings = SETTINGS.changeTo(SETTINGS.gameType(), maxPlayers, SETTINGS.category(), SETTINGS.totalRound(), SETTINGS.difficulty());
        gameRoomManager.createGameRoom(ROOM_ID, settings, HOST);
    }

    private void storeHasRoom() {
        given(gameRoomStore.loadAll()).willReturn(List.of(
                new StoredRoom(ROOM_ID, SETTINGS, GameRoomStatus.WAITING, HOST, List.of(), Instant.now())));
    }

    @Test
    void 마지막_플레이어가_나가면_방과_함께_타이머와_게임세션도_정리한다() {
        // given
        openRoom(8);
        gameRoomManager.startGame(ROOM_ID, HOST);

        // when
        LeaveResult result = gameRoomManager.leaveRoom(ROOM_ID, HOST);

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
        openRoom(8);
        gameRoomManager.joinRoom(ROOM_ID, "참가자");

        // when
        LeaveResult result = gameRoomManager.leaveRoom(ROOM_ID, HOST);

        // then
        assertThat(result.destroyed()).isFalse();
        assertThat(result.wasPlaying()).isFalse();
        verify(gameTimer, never()).stop(ROOM_ID);
        verify(gameSessionManager, never()).endGameSession(ROOM_ID);
    }

    @Test
    void 오래_방치된_방은_게임_상태와_함께_정리된다() {
        // given
        openRoom(8);
        storeHasRoom();
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
        openRoom(8);
        gameRoomManager.leaveRoom(ROOM_ID, HOST);

        // when: 게임 종료 타이머가 뒤늦게 endGame 을 호출하는 상황
        gameRoomManager.endGame(ROOM_ID);

        // then
        verify(gameTimer, times(1)).stop(ROOM_ID);
        verify(gameSessionManager, times(1)).endGameSession(ROOM_ID);
    }

    @Test
    void 진행_중인_방에는_이_게임의_참가자였던_사람만_재입장할_수_있다() {
        // given: 게임 중이고 HOST 가 이탈한 상태
        openRoom(8);
        gameRoomManager.joinRoom(ROOM_ID, "참가자");
        gameRoomManager.readyPlayer(ROOM_ID, "참가자");
        gameRoomManager.startGame(ROOM_ID, HOST);
        gameRoomManager.leaveRoom(ROOM_ID, HOST);

        GameSession session = mock(GameSession.class);
        given(gameSessionManager.getGameSession(ROOM_ID)).willReturn(session);
        given(session.canRejoin(HOST)).willReturn(true);

        // when
        JoinResult result = gameRoomManager.joinRoom(ROOM_ID, HOST);

        // then
        assertThat(result.playerNumber()).isEqualTo(2);
        assertThat(result.newlyJoined()).isTrue();
        verify(session).restorePlayer(HOST);
        assertThat(gameRoomManager.findRoom(ROOM_ID).getRoomPlayers()).contains(HOST);
    }

    @Test
    void 참가자가_아니었던_사람은_진행_중인_방에_들어올_수_없다() {
        // given
        openRoom(8);
        gameRoomManager.startGame(ROOM_ID, HOST);

        GameSession session = mock(GameSession.class);
        given(gameSessionManager.getGameSession(ROOM_ID)).willReturn(session);
        given(session.canRejoin("난입자")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> gameRoomManager.joinRoom(ROOM_ID, "난입자"))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void 아직_이탈_처리되지_않은_플레이어의_재입장은_그대로_통과한다() {
        // given: 새로고침이 이탈 유예보다 빨랐던 경우
        openRoom(8);
        gameRoomManager.startGame(ROOM_ID, HOST);

        // when
        JoinResult result = gameRoomManager.joinRoom(ROOM_ID, HOST);

        // then
        assertThat(result.playerNumber()).isEqualTo(1);
        assertThat(result.newlyJoined()).isFalse();
        verify(gameSessionManager, never()).getGameSession(ROOM_ID);
    }

    @Test
    void 대기_중인_방에_같은_플레이어가_다시_참가해도_새_참가로_집계되지_않는다() {
        // given
        openRoom(8);
        gameRoomManager.joinRoom(ROOM_ID, "참가자");

        // when
        JoinResult result = gameRoomManager.joinRoom(ROOM_ID, "참가자");

        // then
        assertThat(result.newlyJoined()).isFalse();
        assertThat(result.playerNumber()).isEqualTo(2);
        assertThat(gameRoomManager.findRoom(ROOM_ID).getRoomPlayers()).containsExactly(HOST, "참가자");
    }

    @Test
    void 정원이_찬_방이어도_이미_들어와_있는_플레이어의_재참가는_거부하지_않는다() {
        // given
        openRoom(2);
        gameRoomManager.joinRoom(ROOM_ID, "참가자");

        // when
        JoinResult result = gameRoomManager.joinRoom(ROOM_ID, "참가자");

        // then
        assertThat(result.newlyJoined()).isFalse();
        assertThat(result.playerNumber()).isEqualTo(2);
    }

    @Test
    void 없는_방을_touch하면_NPE가_아니라_CoreException이_발생한다() {
        assertThatThrownBy(() -> gameRoomManager.touch(ROOM_ID))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void touch한_방은_유휴_청소_대상에서_제외된다() {
        // given
        openRoom(8);
        storeHasRoom();
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
