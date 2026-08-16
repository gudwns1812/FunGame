package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.domain.member.MemberPresenceService;
import com.fungame.songquiz.domain.quiz.SongReader;
import com.fungame.songquiz.domain.session.GameService;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameRoomServiceTest {

    private static final GamePlayer HOST = GamePlayer.createNewPlayer(1L, "방장");
    private static final GamePlayer GUEST = GamePlayer.createNewPlayer(11L, "참가자");
    private static final GamePlayer LEAVER = GamePlayer.createNewPlayer(12L, "이탈자");

    @Mock
    GameRoomReader gameRoomReader;

    @Mock
    GameRoomWriter gameRoomWriter;

    @Mock
    SongReader songReader;

    @Mock
    GameRoomManager gameRoomManager;

    @Mock
    GameService gameService;

    @Mock
    MemberPresenceService memberPresenceService;

    @Mock
    ApplicationEventPublisher applicationEventPublisher;

    GameRoomService service;

    @BeforeEach
    void setUp() {
        service = new GameRoomService(
                gameRoomManager,
                gameRoomReader,
                gameRoomWriter,
                gameService,
                memberPresenceService,
                applicationEventPublisher
        );
    }

    @Test
    void 방을_만들면_저장소가_발급한_id_로_방을_연다() {
        // given
        RoomSettings settings = new RoomSettings(GameType.SONG, "방2", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD);

        given(gameRoomWriter.open(settings, HOST)).willReturn(7L);

        // when
        Long roomId = service.createRoom(settings, HOST);

        // then
        assertThat(roomId).isEqualTo(7L);

        verify(gameRoomManager).createGameRoom(eq(7L), eq(settings), eq(HOST));
    }

    @Test
    void 실제로_새로_참가했을_때만_입장_이벤트를_발행한다() {
        // given
        given(gameRoomManager.joinRoom(1L, GUEST))
                .willReturn(new JoinResult(2, true));
        GameRoom room = waitingRoom();
        given(gameRoomManager.findRoom(1L)).willReturn(room);

        // when
        int playerNumber = service.joinRoom(1L, GUEST);

        // then
        assertThat(playerNumber).isEqualTo(2);
        verify(applicationEventPublisher).publishEvent(any(PlayerJoinEvent.class));
    }

    @Test
    void 이미_방에_있는_플레이어의_재참가는_입장_이벤트를_발행하지_않는다() {
        // given
        given(gameRoomManager.joinRoom(1L, GUEST))
                .willReturn(new JoinResult(2, false));
        GameRoom room = waitingRoom();
        given(gameRoomManager.findRoom(1L)).willReturn(room);

        // when
        int playerNumber = service.joinRoom(1L, GUEST);

        // then
        assertThat(playerNumber).isEqualTo(2);
        verify(applicationEventPublisher, never()).publishEvent(any(PlayerJoinEvent.class));
    }

    @Test
    void 게임_진행_중_이탈이면_게임별_이탈_처리를_위임한다() {
        // given
        given(gameRoomManager.leaveRoom(1L, LEAVER.memberId()))
                .willReturn(new LeaveResult(false, true, LEAVER.nickname()));

        // when
        service.leaveRoom(1L, LEAVER.memberId());

        // then
        verify(gameService).handlePlayerLeave(1L, LEAVER.memberId());
        verify(applicationEventPublisher).publishEvent(any(PlayerLeaveEvent.class));
    }

    @Test
    void 대기_중_이탈이면_게임_이탈_처리를_하지_않는다() {
        // given
        given(gameRoomManager.leaveRoom(1L, LEAVER.memberId()))
                .willReturn(new LeaveResult(false, false, LEAVER.nickname()));

        // when
        service.leaveRoom(1L, LEAVER.memberId());

        // then
        verify(gameService, never()).handlePlayerLeave(any(), any());
        verify(applicationEventPublisher).publishEvent(any(PlayerLeaveEvent.class));
    }

    @Test
    void 마지막_인원이_나가_방이_사라지면_이탈_이벤트를_발행하지_않는다() {
        // given
        given(gameRoomManager.leaveRoom(1L, LEAVER.memberId()))
                .willReturn(new LeaveResult(true, true, LEAVER.nickname()));

        // when
        service.leaveRoom(1L, LEAVER.memberId());

        // then
        verify(gameService, never()).handlePlayerLeave(any(), any());
        verify(applicationEventPublisher, never()).publishEvent(any(PlayerLeaveEvent.class));
    }

    @Test
    void 방을_만든_사람은_그_방의_대기실에_있는_것으로_기록된다() {
        // given
        RoomSettings settings = new RoomSettings(GameType.SONG, "방2", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD);
        given(gameRoomWriter.open(settings, HOST)).willReturn(7L);

        // when
        service.createRoom(settings, HOST);

        // then
        verify(memberPresenceService).enterWaitingRoom(HOST.memberId(), 7L);
    }

    @Test
    void 대기_중인_방에_입장하면_대기_상태로_기록된다() {
        // given
        given(gameRoomManager.joinRoom(1L, GUEST)).willReturn(new JoinResult(2, true));
        GameRoom room = waitingRoom();
        given(gameRoomManager.findRoom(1L)).willReturn(room);

        // when
        service.joinRoom(1L, GUEST);

        // then
        verify(memberPresenceService).enterWaitingRoom(GUEST.memberId(), 1L);
    }

    @Test
    void 진행_중인_방에_재입장하면_게임중_상태로_기록된다() {
        // given
        given(gameRoomManager.joinRoom(1L, GUEST)).willReturn(new JoinResult(2, true));
        GameRoom room = playingRoom();
        given(gameRoomManager.findRoom(1L)).willReturn(room);

        // when
        service.joinRoom(1L, GUEST);

        // then
        verify(memberPresenceService).enterPlayingRoom(GUEST.memberId(), 1L);
    }

    @Test
    void 방이_사라져도_나간_사람의_위치는_비운다() {
        // given
        given(gameRoomManager.leaveRoom(1L, LEAVER.memberId())).willReturn(new LeaveResult(true, true, LEAVER.nickname()));

        // when
        service.leaveRoom(1L, LEAVER.memberId());

        // then
        verify(memberPresenceService).leaveRoom(LEAVER.memberId());
    }

    @Test
    void 기동_시점에_남아있던_회원_위치를_로비로_되돌린다() {
        // when
        service.resetInterruptedGames();

        // then
        verify(gameRoomWriter).markInterruptedGamesWaiting();
        verify(memberPresenceService).clearEveryLocation();
    }

    private GameRoom waitingRoom() {
        GameRoom room = mock(GameRoom.class);
        given(room.isPlaying()).willReturn(false);
        return room;
    }

    private GameRoom playingRoom() {
        GameRoom room = mock(GameRoom.class);
        given(room.isPlaying()).willReturn(true);
        return room;
    }
}
