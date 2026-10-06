package com.fungame.songquiz.api.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fungame.songquiz.domain.member.MemberConnectionTracker;
import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.MemberLocation;
import com.fungame.songquiz.enums.PlayerStatus;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class RoomLeaveGraceTest {

    private static final Long MEMBER_ID = 11L;
    private static final Long ROOM_ID = 1L;
    private static final Long OTHER_ROOM_ID = 2L;

    @Mock
    GameRoomService gameRoomService;

    @Mock
    TaskScheduler taskScheduler;

    @Mock
    MemberConnectionTracker memberConnectionTracker;

    RoomLeaveGrace roomLeaveGrace;

    @BeforeEach
    void setUp() {
        roomLeaveGrace = new RoomLeaveGrace(gameRoomService, taskScheduler, memberConnectionTracker, 15);
    }

    @Test
    @DisplayName("연결이 끊겨도 유예 시간 안에는 방에서 내보내지 않는다.")
    void doNotEvictWithinGrace() {
        allowScheduling();

        roomLeaveGrace.beginFor(MEMBER_ID);

        verify(gameRoomService, never()).leaveRoom(any(), any());
    }

    @Test
    @DisplayName("유예 시간 안에 돌아오지 않으면 그때의 위치에서 내보낸다.")
    void evictFromCurrentRoomAfterGrace() {
        allowScheduling();
        placeIn(ROOM_ID);
        roomLeaveGrace.beginFor(MEMBER_ID);

        captureScheduledEviction().run();

        verify(gameRoomService).leaveRoom(ROOM_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("유예 중에 다른 방으로 옮겼으면 옮겨간 방에서 내보낸다. 끊긴 시점의 방을 건드리지 않는다.")
    void evictFromTheRoomTheMemberIsInNow() {
        allowScheduling();
        placeIn(ROOM_ID);
        roomLeaveGrace.beginFor(MEMBER_ID);
        Runnable eviction = captureScheduledEviction();

        placeIn(OTHER_ROOM_ID);
        eviction.run();

        verify(gameRoomService).leaveRoom(OTHER_ROOM_ID, MEMBER_ID);
        verify(gameRoomService, never()).leaveRoom(ROOM_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("유예가 만료될 때 이미 로비에 있으면 아무것도 하지 않는다.")
    void skipEvictionWhenAlreadyInLobby() {
        allowScheduling();
        placeIn(ROOM_ID);
        roomLeaveGrace.beginFor(MEMBER_ID);
        Runnable eviction = captureScheduledEviction();

        placeInLobby();
        eviction.run();

        verify(gameRoomService, never()).leaveRoom(any(), any());
    }

    @Test
    @DisplayName("유예가 만료될 때 어느 서버로든 다시 접속해 있으면 방에 그대로 남긴다.")
    void skipEvictionWhenReconnected() {
        allowScheduling();
        roomLeaveGrace.beginFor(MEMBER_ID);
        Runnable eviction = captureScheduledEviction();

        given(memberConnectionTracker.hasLiveConnection(MEMBER_ID)).willReturn(true);
        eviction.run();

        verify(gameRoomService, never()).leaveRoom(any(), any());
    }

    @Test
    @DisplayName("재접속하면 예약된 이탈을 취소한다.")
    void cancelPendingEvictionOnReconnect() {
        ScheduledFuture<?> scheduled = mock(ScheduledFuture.class);
        doReturn(scheduled).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
        roomLeaveGrace.beginFor(MEMBER_ID);

        roomLeaveGrace.cancelFor(MEMBER_ID);

        verify(scheduled).cancel(false);
    }

    @Test
    @DisplayName("끊긴 순간 로비에 있었어도 유예를 걸고, 만료 때 방에 들어가 있으면 그 방에서 내보낸다.")
    void evictFromRoomJoinedAfterDisconnectingInLobby() {
        allowScheduling();
        AtomicReference<MemberLocation> location = new AtomicReference<>(MemberLocation.lobby());
        given(gameRoomService.findLocationOf(MEMBER_ID)).willAnswer(call -> location.get());

        roomLeaveGrace.beginFor(MEMBER_ID);
        location.set(new MemberLocation(PlayerStatus.WAITING, OTHER_ROOM_ID));
        captureScheduledEviction().run();

        verify(gameRoomService).leaveRoom(OTHER_ROOM_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("짧은 시간에 두 번 끊기면 앞선 예약은 취소하고 새로 예약한다.")
    void replacePendingEviction() {
        ScheduledFuture<?> first = mock(ScheduledFuture.class);
        ScheduledFuture<?> second = mock(ScheduledFuture.class);
        doReturn(first, second).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

        roomLeaveGrace.beginFor(MEMBER_ID);
        roomLeaveGrace.beginFor(MEMBER_ID);

        verify(first).cancel(false);
        verify(second, never()).cancel(false);
    }

    private void allowScheduling() {
        doReturn(mock(ScheduledFuture.class))
                .when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    private void placeIn(Long roomId) {
        given(gameRoomService.findLocationOf(MEMBER_ID))
                .willReturn(new MemberLocation(PlayerStatus.WAITING, roomId));
    }

    private void placeInLobby() {
        given(gameRoomService.findLocationOf(MEMBER_ID)).willReturn(MemberLocation.lobby());
    }

    private Runnable captureScheduledEviction() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(captor.capture(), any(Instant.class));

        return captor.getValue();
    }
}
