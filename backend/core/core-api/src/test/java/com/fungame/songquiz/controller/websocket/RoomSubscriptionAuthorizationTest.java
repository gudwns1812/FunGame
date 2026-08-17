package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.support.StompMessages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class RoomSubscriptionAuthorizationTest {

    private static final Long MEMBER_ID = 11L;
    private static final Long ROOM_ID = 7L;

    private final GameRoomService gameRoomService = mock(GameRoomService.class);
    private final RoomSubscriptionAuthorization authorization =
            new RoomSubscriptionAuthorization(gameRoomService);

    @Test
    @DisplayName("방 소속이면 구독을 통과시킨다.")
    void allowSubscriptionOfRoomMember() {
        given(gameRoomService.hasPlayer(ROOM_ID, MEMBER_ID)).willReturn(true);

        assertThat(authorize(StompDestination.room(ROOM_ID), StompMessages.loggedIn(MEMBER_ID))).isNotNull();
    }

    @Test
    @DisplayName("방 소속이 아니면 구독을 등록하지 않는다.")
    void rejectSubscriptionOfOutsider() {
        given(gameRoomService.hasPlayer(ROOM_ID, MEMBER_ID)).willReturn(false);

        assertThat(authorize(StompDestination.room(ROOM_ID), StompMessages.loggedIn(MEMBER_ID))).isNull();
    }

    @Test
    @DisplayName("회원 정보가 없는 세션의 구독은 등록하지 않는다.")
    void rejectSubscriptionWithoutMember() {
        assertThat(authorize(StompDestination.room(ROOM_ID), null)).isNull();
    }

    @Test
    @DisplayName("방 토픽이 아닌 destination 은 인가를 따지지 않는다.")
    void leaveNonRoomDestinationAlone() {
        assertThat(authorize("/topic/lobby", null)).isNotNull();
    }

    private Object authorize(String destination, java.security.Principal user) {
        return authorization.preSend(StompMessages.subscribe("session-1", destination, user), null);
    }
}
