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
import com.fungame.songquiz.domain.member.MemberPresenceChangedEvent;
import com.fungame.songquiz.domain.member.MemberProfileCache;
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
    private final MemberReader memberReader = mock(MemberReader.class);
    private final MemberProfileCache memberProfileCache = new MemberProfileCache(
            memberReader,
            new ConcurrentMapCacheManager(MemberProfileCache.CACHE_NAME));
    private final LobbyNotifier lobbyNotifier = new LobbyNotifier(
            broadcaster, gameRoomService, onlineMemberService, memberProfileCache);

    @Test
    @DisplayName("방이 바뀌면 다시 물어보게 하지 않고 바뀐 방 목록을 로비로 실어 보낸다.")
    void pushRoomsOnRoomChange() {
        List<RoomInfo> rooms = List.of(room());
        given(gameRoomService.findAllRooms()).willReturn(rooms);

        lobbyNotifier.handleRoomChangedEvent(new RoomChangedEvent());
        lobbyNotifier.processPendingUpdate();

        assertThat(sentToLobby()).isEqualTo(RoomResponse.listFrom(rooms, memberProfileCache));
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
    @DisplayName("접속 상태가 바뀌면 접속자 전체 목록을 한 건만 보낸다. 자기 자신은 받는 쪽이 뺀다.")
    void pushWholeOnlineListOnce() {
        given(onlineMemberService.findAllOnline())
                .willReturn(new OnlineMembers(List.of(onlineMember(VIEWER_ID), onlineMember(OTHER_ID))));

        lobbyNotifier.handleMemberPresenceChangedEvent(new MemberPresenceChangedEvent());
        lobbyNotifier.processPendingUpdate();

        assertThat(sentToPresence()).isEqualTo(List.of(
                OnlineMemberResponse.from(onlineMember(VIEWER_ID)),
                OnlineMemberResponse.from(onlineMember(OTHER_ID))));
        verify(broadcaster, never()).sendToUser(any(), any(), any(Object.class));
    }

    @Test
    @DisplayName("접속자 목록은 모두가 구독하는 /topic/presence 로 나간다. 프런트가 이 주소를 구독한다.")
    void presenceGoesToSharedTopic() {
        assertThat(StompDestination.PRESENCE).isEqualTo("/topic/presence");
    }

    @Test
    @DisplayName("한 주기에 몰린 접속 변경은 한 번만 조회해서 한 번만 보낸다.")
    void aggregatePresenceChangesWithinOneCycle() {
        given(onlineMemberService.findAllOnline())
                .willReturn(new OnlineMembers(List.of(onlineMember(VIEWER_ID))));

        lobbyNotifier.handleMemberPresenceChangedEvent(new MemberPresenceChangedEvent());
        lobbyNotifier.handleMemberPresenceChangedEvent(new MemberPresenceChangedEvent());
        lobbyNotifier.processPendingUpdate();

        verify(onlineMemberService, times(1)).findAllOnline();
        verify(broadcaster, times(1)).send(eq(StompDestination.PRESENCE), any(Object.class));
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

    private Object sentToPresence() {
        ArgumentCaptor<ApiResponse<Object>> sent = captor();
        verify(broadcaster).send(eq(StompDestination.PRESENCE), sent.capture());

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
        given(onlineMemberService.findAllOnline()).willReturn(new OnlineMembers(List.of()));
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
