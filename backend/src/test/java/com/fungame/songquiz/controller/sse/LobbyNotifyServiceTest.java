package com.fungame.songquiz.controller.sse;

import com.fungame.songquiz.domain.CSQuizDifficulty;
import com.fungame.songquiz.domain.GameRoomService;
import com.fungame.songquiz.domain.GameRoomStatus;
import com.fungame.songquiz.domain.GameType;
import com.fungame.songquiz.domain.dto.OnlineMemberInfo;
import com.fungame.songquiz.domain.dto.OnlineMembers;
import com.fungame.songquiz.domain.dto.RoomInfo;
import com.fungame.songquiz.domain.event.MemberPresenceChangedEvent;
import com.fungame.songquiz.domain.event.RoomChangedEvent;
import com.fungame.songquiz.domain.member.OnlineMemberService;
import com.fungame.songquiz.domain.member.PlayerStatus;
import com.fungame.songquiz.support.sse.MemberPayload;
import com.fungame.songquiz.support.sse.SseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class LobbyNotifyServiceTest {

    private static final Long VIEWER_ID = 1L;
    private static final Long OTHER_ID = 2L;

    private final SseService sseService = mock(SseService.class);
    private final GameRoomService gameRoomService = mock(GameRoomService.class);
    private final OnlineMemberService onlineMemberService = mock(OnlineMemberService.class);
    private final LobbyNotifyService lobbyNotifyService =
            new LobbyNotifyService(sseService, gameRoomService, onlineMemberService);

    @Test
    @DisplayName("방이 바뀌면 다시 물어보게 하지 않고 바뀐 방 목록을 실어 보낸다.")
    void pushRoomsOnRoomChange() {
        List<RoomInfo> rooms = List.of(room());
        given(gameRoomService.findAllRooms()).willReturn(rooms);

        lobbyNotifyService.handleRoomChangedEvent(new RoomChangedEvent());
        lobbyNotifyService.processPendingUpdate();

        verify(sseService).broadcast("room-update", rooms);
    }

    @Test
    @DisplayName("한 주기에 몰린 방 변경은 한 번만 조회해서 한 번만 보낸다.")
    void aggregateRoomChangesWithinOneCycle() {
        given(gameRoomService.findAllRooms()).willReturn(List.of(room()));

        lobbyNotifyService.handleRoomChangedEvent(new RoomChangedEvent());
        lobbyNotifyService.handleRoomChangedEvent(new RoomChangedEvent());
        lobbyNotifyService.handleRoomChangedEvent(new RoomChangedEvent());
        lobbyNotifyService.processPendingUpdate();

        verify(gameRoomService, times(1)).findAllRooms();
        verify(sseService, times(1)).broadcast(eq("room-update"), any());
    }

    @Test
    @DisplayName("접속 상태가 바뀌면 받는 사람마다 자기 자신을 뺀 목록을 보낸다.")
    void pushOnlineMembersWithoutViewerSelf() {
        given(onlineMemberService.findAllOnline())
                .willReturn(new OnlineMembers(List.of(onlineMember(VIEWER_ID), onlineMember(OTHER_ID))));

        lobbyNotifyService.handleMemberPresenceChangedEvent(new MemberPresenceChangedEvent());
        lobbyNotifyService.processPendingUpdate();

        assertThat(capturedPresencePayload().of(VIEWER_ID))
                .isEqualTo(List.of(onlineMember(OTHER_ID)));
    }

    @Test
    @DisplayName("접속 상태가 바뀌어도 접속자 조회는 한 번만 한다.")
    void lookUpOnlineMembersOnlyOnce() {
        given(onlineMemberService.findAllOnline())
                .willReturn(new OnlineMembers(List.of(onlineMember(VIEWER_ID), onlineMember(OTHER_ID))));

        lobbyNotifyService.handleMemberPresenceChangedEvent(new MemberPresenceChangedEvent());
        lobbyNotifyService.handleMemberPresenceChangedEvent(new MemberPresenceChangedEvent());
        lobbyNotifyService.processPendingUpdate();

        MemberPayload payload = capturedPresencePayload();
        payload.of(VIEWER_ID);
        payload.of(OTHER_ID);

        verify(onlineMemberService, times(1)).findAllOnline();
    }

    @Test
    @DisplayName("바뀐 것이 없으면 조회도 전송도 하지 않는다.")
    void skipWhenNothingChanged() {
        lobbyNotifyService.processPendingUpdate();

        verifyNoInteractions(sseService, gameRoomService, onlineMemberService);
    }

    private MemberPayload capturedPresencePayload() {
        ArgumentCaptor<MemberPayload> payload = ArgumentCaptor.forClass(MemberPayload.class);
        verify(sseService).broadcastEach(eq("presence-update"), payload.capture());

        return payload.getValue();
    }

    private static RoomInfo room() {
        return new RoomInfo(9L, "방 제목", VIEWER_ID, "방장", GameRoomStatus.WAITING, 8, 1, GameType.SONG, CSQuizDifficulty.HARD);
    }

    private static OnlineMemberInfo onlineMember(Long memberId) {
        return new OnlineMemberInfo(memberId, "회원" + memberId, PlayerStatus.LOBBY, null);
    }
}
