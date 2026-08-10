package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.event.PlayerJoinEvent;
import com.fungame.songquiz.domain.event.PlayerLeaveEvent;
import com.fungame.songquiz.domain.gamecreator.SongGameCreateInfo;
import com.fungame.songquiz.domain.gamecreator.SongGameFactory;
import com.fungame.songquiz.storage.CounterEntity;
import com.fungame.songquiz.storage.CounterRepository;
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
    CounterRepository counterRepository;

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
        SongGameFactory gameFactory = new SongGameFactory(songReader);

        service = new GameRoomService(
                counterRepository,
                List.of(gameFactory),
                gameRoomManager,
                gameService,
                applicationEventPublisher
        );
    }

    @Test
    void gameRoom을_만들면_counter가_증가해야한다() {
        // given
        SongGameCreateInfo info = new SongGameCreateInfo(Category.KPOP, 10);

        CounterEntity counter = new CounterEntity(1L, "GAME_ROOM_COUNTER", 0L);
        Game game = mock(Game.class);
        Song song = mock(Song.class);

        given(songReader.findSongByCategoryWithCount(info.category(),info.songCount())).willReturn(List.of(song));
        given(counterRepository.findByName("GAME_ROOM_COUNTER")).willReturn(counter);

        // when
        Long roomId = service.createRoom(GameType.SONG, "방2", 8, "방장", info);

        // then
        assertThat(roomId).isEqualTo(1L);
        assertThat(counter.getCount()).isEqualTo(1L);

        verify(gameRoomManager).createGameRoom(
                eq(1L),
                eq("방2"),
                any(Game.class),
                eq("방장"),
                eq(8)
        );
    }

    @Test
    void 실제로_새로_참가했을_때만_입장_이벤트를_발행한다() {
        // given
        given(gameRoomManager.joinRoom(1L, "참가자"))
                .willReturn(new GameRoom.JoinResult(2, true));

        // when
        int playerNumber = service.joinRoom(1L, "참가자");

        // then
        assertThat(playerNumber).isEqualTo(2);
        verify(applicationEventPublisher).publishEvent(any(PlayerJoinEvent.class));
    }

    @Test
    void 이미_방에_있는_플레이어의_재참가는_입장_이벤트를_발행하지_않는다() {
        // given: 새로고침이나 재연결로 join 이 다시 호출된 경우
        given(gameRoomManager.joinRoom(1L, "참가자"))
                .willReturn(new GameRoom.JoinResult(2, false));

        // when
        int playerNumber = service.joinRoom(1L, "참가자");

        // then: 인원 정보는 그대로 돌려주되 "입장했습니다" 알림은 다시 보내지 않는다
        assertThat(playerNumber).isEqualTo(2);
        verify(applicationEventPublisher, never()).publishEvent(any(PlayerJoinEvent.class));
    }

    @Test
    void 게임_진행_중_이탈이면_게임별_이탈_처리를_위임한다() {
        // given
        given(gameRoomManager.leaveRoom(1L, "이탈자"))
                .willReturn(new GameRoomManager.LeaveResult(false, true));

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
                .willReturn(new GameRoomManager.LeaveResult(false, false));

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
                .willReturn(new GameRoomManager.LeaveResult(true, true));

        // when
        service.leaveRoom(1L, "이탈자");

        // then
        verify(gameService, never()).handlePlayerLeave(any(), any());
        verify(applicationEventPublisher, never()).publishEvent(any(PlayerLeaveEvent.class));
    }
}