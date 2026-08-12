package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.event.PlayerJoinEvent;
import com.fungame.songquiz.domain.event.PlayerLeaveEvent;
import com.fungame.songquiz.domain.gamecreator.SongGameFactory;
import com.fungame.songquiz.storage.GameRoomStore;
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

    @Mock
    GameRoomStore gameRoomStore;

    @Mock
    SongReader songReader;

    @Mock
    GameRoomManager gameRoomManager;

    @Mock
    GameService gameService;

    @Mock
    ApplicationEventPublisher applicationEventPublisher;

    GameRoomService service;

    @BeforeEach
    void setUp() {
        service = new GameRoomService(
                gameRoomManager,
                gameRoomStore,
                gameService,
                new RoomPresence(),
                applicationEventPublisher
        );
    }

    @Test
    void 방을_만들면_저장소가_발급한_id_로_방을_연다() {
        // given
        RoomSettings settings = new RoomSettings(GameType.SONG, "방2", 8, Category.KPOP, 10, 0);

        given(gameRoomStore.open(settings, "방장")).willReturn(7L);

        // when
        Long roomId = service.createRoom(settings, "방장");

        // then
        assertThat(roomId).isEqualTo(7L);

        verify(gameRoomManager).createGameRoom(eq(7L), eq(settings), eq("방장"));
    }

    @Test
    void 실제로_새로_참가했을_때만_입장_이벤트를_발행한다() {
        // given
        given(gameRoomManager.joinRoom(1L, "참가자"))
                .willReturn(new JoinResult(2, true));

        // when
        int playerNumber = service.joinRoom(1L, "참가자");

        // then
        assertThat(playerNumber).isEqualTo(2);
        verify(applicationEventPublisher).publishEvent(any(PlayerJoinEvent.class));
    }

    @Test
    void 이미_방에_있는_플레이어의_재참가는_입장_이벤트를_발행하지_않는다() {
        // given
        given(gameRoomManager.joinRoom(1L, "참가자"))
                .willReturn(new JoinResult(2, false));

        // when
        int playerNumber = service.joinRoom(1L, "참가자");

        // then
        assertThat(playerNumber).isEqualTo(2);
        verify(applicationEventPublisher, never()).publishEvent(any(PlayerJoinEvent.class));
    }

    @Test
    void 게임_진행_중_이탈이면_게임별_이탈_처리를_위임한다() {
        // given
        given(gameRoomManager.leaveRoom(1L, "이탈자"))
                .willReturn(new LeaveResult(false, true));

        // when
        service.leaveRoom(1L, "이탈자");

        // then
        verify(gameService).handlePlayerLeave(1L, "이탈자");
        verify(applicationEventPublisher).publishEvent(any(PlayerLeaveEvent.class));
    }

    @Test
    void 대기_중_이탈이면_게임_이탈_처리를_하지_않는다() {
        // given
        given(gameRoomManager.leaveRoom(1L, "이탈자"))
                .willReturn(new LeaveResult(false, false));

        // when
        service.leaveRoom(1L, "이탈자");

        // then
        verify(gameService, never()).handlePlayerLeave(any(), any());
        verify(applicationEventPublisher).publishEvent(any(PlayerLeaveEvent.class));
    }

    @Test
    void 마지막_인원이_나가_방이_사라지면_이탈_이벤트를_발행하지_않는다() {
        // given
        given(gameRoomManager.leaveRoom(1L, "이탈자"))
                .willReturn(new LeaveResult(true, true));

        // when
        service.leaveRoom(1L, "이탈자");

        // then
        verify(gameService, never()).handlePlayerLeave(any(), any());
        verify(applicationEventPublisher, never()).publishEvent(any(PlayerLeaveEvent.class));
    }
}