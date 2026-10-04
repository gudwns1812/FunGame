package com.fungame.songquiz.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fungame.songquiz.api.controller.response.ApiResponse;
import com.fungame.songquiz.api.controller.response.OnlineMemberResponse;
import com.fungame.songquiz.api.controller.response.RoomResponse;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.member.MemberPresenceChangedEvent;
import com.fungame.songquiz.domain.member.MemberProfiles;
import com.fungame.songquiz.domain.member.MemberReader;
import com.fungame.songquiz.domain.member.OnlineMemberInfo;
import com.fungame.songquiz.domain.member.OnlineMemberService;
import com.fungame.songquiz.domain.member.OnlineMembers;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.RoomChangedEvent;
import com.fungame.songquiz.domain.room.RoomInfo;
import com.fungame.songquiz.domain.room.RoomSettings;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameRoomStatus;
import com.fungame.songquiz.enums.GameType;
import com.fungame.songquiz.enums.PlayerStatus;
import com.fungame.songquiz.support.MemberFixture;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

class LobbyNotifierTest {

    private static final Long VIEWER_ID = 1L;
    private static final Long OTHER_ID = 2L;

    private final StompBroadcaster broadcaster = mock(StompBroadcaster.class);
    private final GameRoomService gameRoomService = mock(GameRoomService.class);
    private final OnlineMemberService onlineMemberService = mock(OnlineMemberService.class);
    private final StompSessions stompSessions = new StompSessions();
    private final MemberReader memberReader = mock(MemberReader.class);
    private final MemberProfiles memberProfiles = new MemberProfiles(
            memberReader,
            new ConcurrentMapCacheManager(MemberProfiles.CACHE_NAME));
    private final LobbyNotifier lobbyNotifier = new LobbyNotifier(
            broadcaster, gameRoomService, onlineMemberService, memberProfiles, stompSessions);

    @Test
    @DisplayName("방이 바뀌면 다시 물어보게 하지 않고 바뀐 방 목록을 로비로 실어 보낸다.")
    void pushRoomsOnRoomChange() {
        List<RoomInfo> rooms = List.of(room());
        given(gameRoomService.findAllRooms()).willReturn(rooms);

        lobbyNotifier.handleRoomChangedEvent(new RoomChangedEvent());
        lobbyNotifier.processPendingUpdate();

        assertThat(sentToLobby()).isEqualTo(RoomResponse.listFrom(rooms, memberProfiles));
    }

    @Test
    @DisplayName("한 주기에 몰린 방 변경은 한 번만 조회해서 한 번만 보낸다.")
    void aggregateRoomChangesWithinOneCycle() {
        given(gameRoomService.findAllRooms()).willReturn(List.of(room()));

        lobbyNotifier.handleRoomChangedEvent(new RoomChangedEvent());
        lobbyNotifier.handleRoomChangedEvent(new RoomChangedEvent());
        lobbyNotifier.handleRoomChangedEvent(new RoomChangedEvent());
        lobbyNotifier.processPendingUpdate();

        verify(gameRoomService, times(1)).findAllRooms();
        verify(broadcaster, times(1)).send(eq(StompDestination.LOBBY), any(Object.class));
    }

    @Test
    @DisplayName("접속 상태가 바뀌면 접속 중인 사람마다 자기 자신을 뺀 목록을 보낸다.")
    void pushOnlineMembersWithoutViewerSelf() {
        stompSessions.add("session-1", VIEWER_ID);
        stompSessions.add("session-2", OTHER_ID);
        given(onlineMemberService.findAllOnline())
                .willReturn(new OnlineMembers(List.of(onlineMember(VIEWER_ID), onlineMember(OTHER_ID))));

        lobbyNotifier.handleMemberPresenceChangedEvent(new MemberPresenceChangedEvent());
        lobbyNotifier.processPendingUpdate();

        assertThat(sentToViewer(VIEWER_ID)).isEqualTo(List.of(OnlineMemberResponse.from(onlineMember(OTHER_ID))));
        assertThat(sentToViewer(OTHER_ID)).isEqualTo(List.of(OnlineMemberResponse.from(onlineMember(VIEWER_ID))));
    }

    @Test
    @DisplayName("접속 상태가 바뀌어도 접속자 조회는 한 번만 한다.")
    void lookUpOnlineMembersOnlyOnce() {
        stompSessions.add("session-1", VIEWER_ID);
        stompSessions.add("session-2", VIEWER_ID);
        given(onlineMemberService.findAllOnline())
                .willReturn(new OnlineMembers(List.of(onlineMember(VIEWER_ID))));

        lobbyNotifier.handleMemberPresenceChangedEvent(new MemberPresenceChangedEvent());
        lobbyNotifier.handleMemberPresenceChangedEvent(new MemberPresenceChangedEvent());
        lobbyNotifier.processPendingUpdate();

        verify(onlineMemberService, times(1)).findAllOnline();
    }

    @Test
    @DisplayName("탭을 여러 개 열어도 회원 한 명에게는 한 번만 보낸다. 세션 배달은 브로커가 맡는다.")
    void sendOncePerMemberNotPerSession() {
        stompSessions.add("session-1", VIEWER_ID);
        stompSessions.add("session-2", VIEWER_ID);
        given(onlineMemberService.findAllOnline())
                .willReturn(new OnlineMembers(List.of(onlineMember(VIEWER_ID))));

        lobbyNotifier.handleMemberPresenceChangedEvent(new MemberPresenceChangedEvent());
        lobbyNotifier.processPendingUpdate();

        verify(broadcaster, times(1)).sendToUser(
                eq(MemberAdapter.principalNameOf(VIEWER_ID)), eq(StompDestination.PRESENCE), any(Object.class));
    }

    @Test
    @DisplayName("아무도 접속해 있지 않으면 접속자 목록을 보내지 않는다.")
    void skipPresenceWithoutAnyConnection() {
        given(onlineMemberService.findAllOnline()).willReturn(new OnlineMembers(List.of()));

        lobbyNotifier.handleMemberPresenceChangedEvent(new MemberPresenceChangedEvent());
        lobbyNotifier.processPendingUpdate();

        verify(broadcaster, never()).sendToUser(any(), any(), any(Object.class));
    }

    @Test
    @DisplayName("바뀐 것이 없으면 조회도 전송도 하지 않는다.")
    void skipWhenNothingChanged() {
        lobbyNotifier.processPendingUpdate();

        verifyNoInteractions(broadcaster, gameRoomService, onlineMemberService);
    }

    private Object sentToLobby() {
        ArgumentCaptor<ApiResponse<Object>> sent = captor();
        verify(broadcaster).send(eq(StompDestination.LOBBY), sent.capture());

        return sent.getValue().getData();
    }

    private Object sentToViewer(Long viewerId) {
        ArgumentCaptor<ApiResponse<Object>> sent = captor();
        verify(broadcaster).sendToUser(
                eq(MemberAdapter.principalNameOf(viewerId)), eq(StompDestination.PRESENCE), sent.capture());

        return sent.getValue().getData();
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<ApiResponse<Object>> captor() {
        return ArgumentCaptor.forClass(ApiResponse.class);
    }

    @BeforeEach
    void nameEveryMember() {
        BDDMockito.given(memberReader.findMember(ArgumentMatchers.anyLong()))
                .willAnswer(call -> MemberFixture.withId(call.getArgument(0), "회원" + call.getArgument(0)));
    }

    private static RoomInfo room() {
        return new RoomInfo(9L,
                new RoomSettings(GameType.SONG, "방 제목", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD),
                GamePlayer.createNewPlayer(VIEWER_ID), GameRoomStatus.WAITING, 1);
    }

    private static OnlineMemberInfo onlineMember(Long memberId) {
        return new OnlineMemberInfo(memberId, "회원" + memberId, PlayerStatus.LOBBY, null);
    }
}
