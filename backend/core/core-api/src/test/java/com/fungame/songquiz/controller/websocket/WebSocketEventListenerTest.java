package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.support.StompMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    private static final Long MEMBER_ID = 11L;

    @Mock
    RoomLeaveGrace roomLeaveGrace;

    StompSessions stompSessions;
    WebSocketEventListener listener;

    @BeforeEach
    void setUp() {
        stompSessions = new StompSessions();
        listener = new WebSocketEventListener(stompSessions, roomLeaveGrace);
    }

    @Test
    @DisplayName("접속하면 세션을 회원에 묶고 예약된 이탈을 취소한다.")
    void registerSessionAndCancelPendingLeaveOnConnect() {
        connect("session-1");

        assertThat(stompSessions.isConnected(MEMBER_ID)).isTrue();
        verify(roomLeaveGrace).cancelFor(MEMBER_ID);
    }

    @Test
    @DisplayName("회원 정보가 없는 세션은 등록하지 않는다.")
    void ignoreSessionWithoutMember() {
        listener.handleConnected(new SessionConnectedEvent(this, StompMessages.session("session-1"), null));

        assertThat(stompSessions.connectedMemberIds()).isEmpty();
        verify(roomLeaveGrace, never()).cancelFor(MEMBER_ID);
    }

    @Test
    @DisplayName("마지막 세션이 끊기면 이탈 유예를 건다.")
    void beginGraceWhenLastSessionCloses() {
        connect("session-1");

        disconnect("session-1");

        assertThat(stompSessions.isConnected(MEMBER_ID)).isFalse();
        verify(roomLeaveGrace).beginFor(MEMBER_ID);
    }

    @Test
    @DisplayName("다른 탭이 살아 있으면 이탈 유예를 걸지 않는다.")
    void doNotBeginGraceWhileAnotherTabIsOpen() {
        connect("session-1");
        connect("session-2");

        disconnect("session-1");

        verify(roomLeaveGrace, never()).beginFor(MEMBER_ID);
    }

    @Test
    @DisplayName("등록된 적 없는 세션의 종료는 무시한다.")
    void ignoreUnknownSessionDisconnect() {
        disconnect("등록된적-없는-세션");

        verify(roomLeaveGrace, never()).beginFor(MEMBER_ID);
    }

    private void connect(String sessionId) {
        listener.handleConnected(new SessionConnectedEvent(
                this, StompMessages.session(sessionId), StompMessages.loggedIn(MEMBER_ID)));
    }

    private void disconnect(String sessionId) {
        listener.handleDisconnect(new SessionDisconnectEvent(
                this, StompMessages.session(sessionId), sessionId, CloseStatus.NORMAL));
    }
}
