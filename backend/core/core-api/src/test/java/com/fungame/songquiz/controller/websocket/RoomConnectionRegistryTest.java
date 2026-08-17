package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.controller.room.RoomMember;
import com.fungame.songquiz.controller.room.RoomPresence;
import com.fungame.songquiz.domain.room.GameRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoomConnectionRegistryTest {

    private static final Long ROOM_ID = 1L;
    private static final String NICKNAME = "참가자";
    private static final Long MEMBER_ID = 11L;
    private static final RoomMember MEMBER = new RoomMember(ROOM_ID, MEMBER_ID, NICKNAME);

    @Mock
    GameRoomService gameRoomService;

    @Mock
    TaskScheduler taskScheduler;

    RoomConnectionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new RoomConnectionRegistry(gameRoomService, taskScheduler, new RoomPresence());
    }

    private Runnable captureScheduledLeave() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(captor.capture(), any(Instant.class));
        return captor.getValue();
    }

    private void allowScheduling() {
        doReturn(mock(ScheduledFuture.class))
                .when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    private void stillInRoom() {
        given(gameRoomService.hasPlayer(ROOM_ID, MEMBER_ID)).willReturn(true);
    }

    @Test
    void 연결이_끊겨도_유예_시간_안에는_방에서_내보내지_않는다() {
        // given
        allowScheduling();
        stillInRoom();
        registry.connected("session-1", MEMBER);

        // when
        registry.disconnected("session-1");

        // then
        verify(gameRoomService, never()).leaveRoom(ROOM_ID, MEMBER_ID);
        assertThat(registry.isConnected(MEMBER)).isFalse();
    }

    @Test
    void 유예_시간_안에_재연결하면_방에_그대로_남는다() {
        // given
        allowScheduling();
        stillInRoom();
        registry.connected("session-1", MEMBER);
        registry.disconnected("session-1");
        Runnable scheduledLeave = captureScheduledLeave();

        // when
        registry.connected("session-2", MEMBER);
        scheduledLeave.run();

        // then
        assertThat(registry.isConnected(MEMBER)).isTrue();
        verify(gameRoomService, never()).leaveRoom(ROOM_ID, MEMBER_ID);
    }

    @Test
    void 유예_시간_안에_돌아오지_않으면_그때_방에서_내보낸다() {
        // given
        allowScheduling();
        stillInRoom();
        registry.connected("session-1", MEMBER);
        registry.disconnected("session-1");

        // when
        captureScheduledLeave().run();

        // then
        verify(gameRoomService).leaveRoom(ROOM_ID, MEMBER_ID);
    }

    @Test
    void 방을_구독하지_않은_세션의_연결_종료는_무시한다() {
        // when
        registry.disconnected("등록된적-없는-세션");

        // then
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
        verify(gameRoomService, never()).leaveRoom(any(), any());
    }

    @Test
    void 다른_연결이_살아있으면_이탈을_예약하지_않는다() {
        // given
        registry.connected("session-1", MEMBER);
        registry.connected("session-2", MEMBER);

        // when
        registry.disconnected("session-1");

        // then
        assertThat(registry.isConnected(MEMBER)).isTrue();
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void 이미_방에서_빠진_사람의_연결_종료는_이탈을_예약하지_않는다() {
        // given
        given(gameRoomService.hasPlayer(ROOM_ID, MEMBER_ID)).willReturn(false);
        registry.connected("session-1", MEMBER);

        // when
        registry.disconnected("session-1");

        // then
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
        verify(gameRoomService, never()).leaveRoom(any(), any());
    }

    @Test
    void 짧은_시간에_두_번_끊기면_앞선_예약은_취소하고_새로_예약한다() {
        // given
        ScheduledFuture<?> first = mock(ScheduledFuture.class);
        ScheduledFuture<?> second = mock(ScheduledFuture.class);
        doReturn(first, second)
                .when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
        stillInRoom();

        registry.connected("session-1", MEMBER);
        registry.disconnected("session-1");

        // when
        registry.connected("session-2", MEMBER);
        registry.disconnected("session-2");

        // then
        verify(first).cancel(false);
        verify(second, never()).cancel(false);
    }
}
