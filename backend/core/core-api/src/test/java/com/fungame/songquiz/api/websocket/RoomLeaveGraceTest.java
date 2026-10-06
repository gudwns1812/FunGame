package com.fungame.songquiz.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fungame.songquiz.domain.member.MemberConnectionTracker;
import com.fungame.songquiz.domain.member.MembersWentOfflineEvent;
import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.MemberLocation;
import com.fungame.songquiz.domain.session.GameTimer;
import com.fungame.songquiz.domain.session.GameTimerTask;
import com.fungame.songquiz.enums.PlayerStatus;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomLeaveGraceTest {

    private static final Long MEMBER_ID = 11L;
    private static final Long ROOM_ID = 1L;
    private static final Long OTHER_ROOM_ID = 2L;
    private static final Duration GRACE = Duration.ofSeconds(15);

    @Mock
    GameRoomService gameRoomService;

    @Mock
    GameTimer gameTimer;

    @Mock
    MemberConnectionTracker memberConnectionTracker;

    RoomLeaveGrace roomLeaveGrace;

    @BeforeEach
    void setUp() {
        roomLeaveGrace = new RoomLeaveGrace(gameRoomService, gameTimer, memberConnectionTracker, GRACE.toSeconds());
    }

    @Test
    @DisplayName("연결이 모두 끊기면 유예 시간 뒤의 퇴장을 공유 타이머에 예약한다. 끊긴 서버가 죽어도 예약은 남는다.")
    void schedulesLeaveOnTheSharedTimer() {
        roomLeaveGrace.beginFor(MEMBER_ID);

        verify(gameTimer).startAfter(GRACE, GameTimerTask.leaveRoom(MEMBER_ID));
        verify(gameRoomService, never()).leaveRoom(any(), any());
    }

    @Test
    @DisplayName("끊긴 순간의 위치를 보지 않는다. 그 순간 로비였어도 예약하고, 내보낼지는 유예가 끝날 때 정한다.")
    void scheduleWithoutLookingAtLocationWhenDisconnected() {
        roomLeaveGrace.beginFor(MEMBER_ID);

        verify(gameRoomService, never()).findLocationOf(any());
        verify(gameTimer).startAfter(GRACE, GameTimerTask.leaveRoom(MEMBER_ID));
    }

    @Test
    @DisplayName("재접속하면 예약된 퇴장을 지운다.")
    void cancelPendingLeaveOnReconnect() {
        roomLeaveGrace.cancelFor(MEMBER_ID);

        verify(gameTimer).cancel(GameTimerTask.leaveRoom(MEMBER_ID));
    }

    @Test
    @DisplayName("유예 시간 안에 돌아오지 않으면 그때의 위치에서 내보낸다.")
    void evictFromCurrentRoomAfterGrace() {
        placeIn(ROOM_ID);

        roomLeaveGrace.onTimer(GameTimerTask.leaveRoom(MEMBER_ID));

        verify(gameRoomService).leaveRoom(ROOM_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("유예 중에 다른 방으로 옮겼으면 옮겨간 방에서 내보낸다.")
    void evictFromTheRoomTheMemberIsInNow() {
        placeIn(OTHER_ROOM_ID);

        roomLeaveGrace.onTimer(GameTimerTask.leaveRoom(MEMBER_ID));

        verify(gameRoomService).leaveRoom(OTHER_ROOM_ID, MEMBER_ID);
        verify(gameRoomService, never()).leaveRoom(ROOM_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("유예가 끝날 때 이미 로비에 있으면 아무것도 하지 않는다.")
    void skipEvictionWhenAlreadyInLobby() {
        placeInLobby();

        roomLeaveGrace.onTimer(GameTimerTask.leaveRoom(MEMBER_ID));

        verify(gameRoomService, never()).leaveRoom(any(), any());
    }

    @Test
    @DisplayName("유예가 끝날 때 어느 서버로든 다시 접속해 있으면 방에 그대로 남긴다.")
    void skipEvictionWhenReconnected() {
        given(memberConnectionTracker.hasLiveConnection(MEMBER_ID)).willReturn(true);

        roomLeaveGrace.onTimer(GameTimerTask.leaveRoom(MEMBER_ID));

        verify(gameRoomService, never()).leaveRoom(any(), any());
    }

    @Test
    @DisplayName("오프라인이 된 회원은 그때 있는 방에서 내보낸다. 연결이 붙어 있던 서버가 죽어도 빠진다.")
    void evictMembersWhoWentOffline() {
        placeIn(ROOM_ID);

        roomLeaveGrace.handleMembersWentOffline(new MembersWentOfflineEvent(Set.of(MEMBER_ID)));

        verify(gameRoomService).leaveRoom(ROOM_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("오프라인으로 알려졌어도 그사이 다시 접속했으면 내보내지 않는다.")
    void keepMembersWhoCameBackBeforeEviction() {
        given(memberConnectionTracker.hasLiveConnection(MEMBER_ID)).willReturn(true);

        roomLeaveGrace.handleMembersWentOffline(new MembersWentOfflineEvent(Set.of(MEMBER_ID)));

        verify(gameRoomService, never()).leaveRoom(any(), any());
    }

    @Test
    @DisplayName("퇴장 예약만 맡는다.")
    void handlesOnlyLeaveTasks() {
        assertThat(roomLeaveGrace.timerKinds()).containsExactly(GameTimerTask.Kind.LEAVE_ROOM);
    }

    private void placeIn(Long roomId) {
        given(gameRoomService.findLocationOf(MEMBER_ID))
                .willReturn(new MemberLocation(PlayerStatus.WAITING, roomId));
    }

    private void placeInLobby() {
        given(gameRoomService.findLocationOf(MEMBER_ID)).willReturn(MemberLocation.lobby());
    }
}
