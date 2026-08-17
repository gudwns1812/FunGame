package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.domain.session.GameSession;
import com.fungame.songquiz.domain.session.GameSessionManager;
import com.fungame.songquiz.domain.session.GameTimer;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.enums.PlayerStatus;
import com.fungame.songquiz.support.error.CoreException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameRoomManagerTest {

    private static final Long MISSING_ROOM_ID = 999L;
    private static final GamePlayer HOST = GamePlayer.createNewPlayer(1L, "방장");
    private static final GamePlayer GUEST = GamePlayer.createNewPlayer(2L, "참가자");
    private static final GamePlayer INTRUDER = GamePlayer.createNewPlayer(9L, "난입자");
    private static final RoomSettings SETTINGS = new RoomSettings(GameType.SONG, "방", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD);

    @Mock
    ApplicationEventPublisher applicationEventPublisher;

    @Mock
    GameTimer gameTimer;

    @Mock
    GameSessionManager gameSessionManager;

    GameRoomManager gameRoomManager;

    Long roomId;

    @BeforeEach
    void setUp() {
        gameRoomManager = new GameRoomManager(
                new LockContext(),
                applicationEventPublisher,
                gameTimer,
                gameSessionManager
        );
    }

    private void openRoom(int maxPlayers) {
        RoomSettings settings = SETTINGS.changeTo(SETTINGS.gameType(), maxPlayers, SETTINGS.category(), SETTINGS.totalRound(), SETTINGS.difficulty(), SETTINGS.csDifficulty());
        roomId = gameRoomManager.createGameRoom(settings, HOST);
    }

    @Test
    void 방_번호는_만들어질_때마다_새로_발급된다() {
        openRoom(8);
        Long firstRoomId = roomId;

        openRoom(8);

        assertThat(roomId).isNotEqualTo(firstRoomId);
        assertThat(gameRoomManager.findRoom(firstRoomId)).isNotNull();
        assertThat(gameRoomManager.findRoom(roomId)).isNotNull();
    }

    @Test
    void 마지막_플레이어가_나가면_방과_함께_타이머와_게임세션도_정리한다() {
        // given
        openRoom(8);
        gameRoomManager.startGame(roomId, HOST.memberId());

        // when
        LeaveResult result = gameRoomManager.leaveRoom(roomId, HOST.memberId());

        // then
        assertThat(result.destroyed()).isTrue();
        assertThat(result.wasPlaying()).isTrue();
        verify(gameTimer).stop(roomId);
        verify(gameSessionManager).endGameSession(roomId);
        assertThatThrownBy(() -> gameRoomManager.findRoom(roomId))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void 남은_플레이어가_있으면_게임_상태를_정리하지_않는다() {
        // given
        openRoom(8);
        gameRoomManager.joinRoom(roomId, GUEST);

        // when
        LeaveResult result = gameRoomManager.leaveRoom(roomId, HOST.memberId());

        // then
        assertThat(result.destroyed()).isFalse();
        assertThat(result.wasPlaying()).isFalse();
        verify(gameTimer, never()).stop(roomId);
        verify(gameSessionManager, never()).endGameSession(roomId);
    }

    @Test
    void 오래_방치된_방은_게임_상태와_함께_정리된다() {
        // given
        openRoom(8);
        makeIdle(roomId);

        // when
        gameRoomManager.cleanupIdleRooms();

        // then
        verify(gameTimer).stop(roomId);
        verify(gameSessionManager).endGameSession(roomId);
        assertThatThrownBy(() -> gameRoomManager.findRoom(roomId))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void 이미_정리된_방을_다시_삭제해도_정리가_중복_수행되지_않는다() {
        // given
        openRoom(8);
        gameRoomManager.leaveRoom(roomId, HOST.memberId());

        // when: 게임 종료 타이머가 뒤늦게 endGame 을 호출하는 상황
        gameRoomManager.endGame(roomId);

        // then
        verify(gameTimer, times(1)).stop(roomId);
        verify(gameSessionManager, times(1)).endGameSession(roomId);
    }

    @Test
    void 진행_중인_방에는_이_게임의_참가자였던_사람만_재입장할_수_있다() {
        // given: 게임 중이고 HOST 가 이탈한 상태
        openRoom(8);
        gameRoomManager.joinRoom(roomId, GUEST);
        gameRoomManager.readyPlayer(roomId, GUEST.memberId());
        gameRoomManager.startGame(roomId, HOST.memberId());
        gameRoomManager.leaveRoom(roomId, HOST.memberId());

        GameSession session = mock(GameSession.class);
        given(gameSessionManager.getGameSession(roomId)).willReturn(session);
        given(session.canRejoin(HOST.memberId())).willReturn(true);

        // when
        JoinResult result = gameRoomManager.joinRoom(roomId, HOST);

        // then
        assertThat(result.playerNumber()).isEqualTo(2);
        assertThat(result.newlyJoined()).isTrue();
        verify(session).restorePlayer(HOST);
        assertThat(gameRoomManager.findRoom(roomId).getRoomPlayers())
                .extracting(GamePlayer::memberId)
                .contains(HOST.memberId());
    }

    @Test
    void 참가자가_아니었던_사람은_진행_중인_방에_들어올_수_없다() {
        // given
        openRoom(8);
        gameRoomManager.startGame(roomId, HOST.memberId());

        GameSession session = mock(GameSession.class);
        given(gameSessionManager.getGameSession(roomId)).willReturn(session);
        given(session.canRejoin(INTRUDER.memberId())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> gameRoomManager.joinRoom(roomId, INTRUDER))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void 아직_이탈_처리되지_않은_플레이어의_재입장은_그대로_통과한다() {
        // given: 새로고침이 이탈 유예보다 빨랐던 경우
        openRoom(8);
        gameRoomManager.startGame(roomId, HOST.memberId());

        // when
        JoinResult result = gameRoomManager.joinRoom(roomId, HOST);

        // then
        assertThat(result.playerNumber()).isEqualTo(1);
        assertThat(result.newlyJoined()).isFalse();
        verify(gameSessionManager, never()).getGameSession(roomId);
    }

    @Test
    void 대기_중인_방에_같은_플레이어가_다시_참가해도_새_참가로_집계되지_않는다() {
        // given
        openRoom(8);
        gameRoomManager.joinRoom(roomId, GUEST);

        // when
        JoinResult result = gameRoomManager.joinRoom(roomId, GUEST);

        // then
        assertThat(result.newlyJoined()).isFalse();
        assertThat(result.playerNumber()).isEqualTo(2);
        assertThat(gameRoomManager.findRoom(roomId).getRoomPlayers())
                .extracting(GamePlayer::memberId)
                .containsExactly(HOST.memberId(), GUEST.memberId());
    }

    @Test
    void 정원이_찬_방이어도_이미_들어와_있는_플레이어의_재참가는_거부하지_않는다() {
        // given
        openRoom(2);
        gameRoomManager.joinRoom(roomId, GUEST);

        // when
        JoinResult result = gameRoomManager.joinRoom(roomId, GUEST);

        // then
        assertThat(result.newlyJoined()).isFalse();
        assertThat(result.playerNumber()).isEqualTo(2);
    }

    @Test
    void 강퇴한_플레이어는_방에서_빠지고_방이_바뀌었음을_알린다() {
        // given
        openRoom(8);
        gameRoomManager.joinRoom(roomId, GUEST);
        clearInvocations(applicationEventPublisher);

        // when
        KickResult result = gameRoomManager.kickPlayer(roomId, HOST.memberId(), GUEST.memberId());

        // then
        assertThat(result.kicked().memberId()).isEqualTo(GUEST.memberId());
        assertThat(gameRoomManager.findRoom(roomId).getRoomPlayers())
                .extracting(GamePlayer::memberId)
                .containsExactly(HOST.memberId());
        verify(applicationEventPublisher).publishEvent(any(RoomChangedEvent.class));
    }

    @Test
    void 방에_있는_사람과_없는_사람을_구분한다() {
        // given
        openRoom(8);
        gameRoomManager.joinRoom(roomId, GUEST);

        // when // then
        assertThat(gameRoomManager.hasPlayer(roomId, GUEST.memberId())).isTrue();
        assertThat(gameRoomManager.hasPlayer(roomId, INTRUDER.memberId())).isFalse();
    }

    @Test
    void 없는_방은_참가자가_없다고_답한다() {
        assertThat(gameRoomManager.hasPlayer(MISSING_ROOM_ID, GUEST.memberId())).isFalse();
    }

    @Test
    void 없는_방을_touch하면_NPE가_아니라_CoreException이_발생한다() {
        assertThatThrownBy(() -> gameRoomManager.touch(MISSING_ROOM_ID))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void touch한_방은_유휴_청소_대상에서_제외된다() {
        // given
        openRoom(8);
        makeIdle(roomId);

        // when
        gameRoomManager.touch(roomId);
        gameRoomManager.cleanupIdleRooms();

        // then
        assertThat(gameRoomManager.findRoom(roomId)).isNotNull();
        verify(gameTimer, never()).stop(roomId);
    }

    @Test
    void 대기_중인_방에_있는_사람의_위치는_대기중이다() {
        openRoom(8);
        gameRoomManager.joinRoom(roomId, GUEST);

        MemberLocation location = gameRoomManager.locationOf(GUEST.memberId());

        assertThat(location.status()).isEqualTo(PlayerStatus.WAITING);
        assertThat(location.roomId()).isEqualTo(roomId);
    }

    @Test
    void 게임이_시작되면_방에_있는_사람의_위치도_게임중으로_바뀐다() {
        openRoom(8);
        gameRoomManager.joinRoom(roomId, GUEST);
        gameRoomManager.readyPlayer(roomId, GUEST.memberId());
        gameRoomManager.startGame(roomId, HOST.memberId());

        MemberLocations locations = gameRoomManager.locationsOfEveryPlayer();

        assertThat(locations.of(HOST.memberId()).status()).isEqualTo(PlayerStatus.PLAYING);
        assertThat(locations.of(GUEST.memberId()).status()).isEqualTo(PlayerStatus.PLAYING);
    }

    @Test
    void 어느_방에도_없는_사람의_위치는_로비다() {
        openRoom(8);

        assertThat(gameRoomManager.locationOf(INTRUDER.memberId()).isInLobby()).isTrue();
        assertThat(gameRoomManager.locationsOfEveryPlayer().of(INTRUDER.memberId()).isInLobby()).isTrue();
    }

    @Test
    void 방을_나가면_위치도_로비로_돌아간다() {
        openRoom(8);
        gameRoomManager.joinRoom(roomId, GUEST);

        gameRoomManager.leaveRoom(roomId, GUEST.memberId());

        assertThat(gameRoomManager.locationOf(GUEST.memberId()).isInLobby()).isTrue();
    }

    private void makeIdle(Long roomId) {
        GameRoom room = gameRoomManager.findRoom(roomId);
        ReflectionTestUtils.setField(room, "lastActivityTime", Instant.now().minus(31, ChronoUnit.MINUTES));
    }
}
