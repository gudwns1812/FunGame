package com.fungame.songquiz.controller.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * 서비스 접속 여부만 다룬다. 방 소속은 join / leave API 가 정하므로 SUBSCRIBE 는 보지 않는다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final StompSessions stompSessions;
    private final RoomLeaveGrace roomLeaveGrace;

    @EventListener
    public void handleConnected(SessionConnectedEvent event) {
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
        Long memberId = StompUser.memberIdOf(event.getUser());
        if (memberId == null) {
            log.warn("세션 {} 의 회원 정보를 확인할 수 없다", sessionId);
            return;
        }

        stompSessions.add(sessionId, memberId);
        roomLeaveGrace.cancelFor(memberId);
        log.debug("접속: 회원 {} (session {}), 열린 세션 {} 개",
                memberId, sessionId, stompSessions.countSessionsOf(memberId));
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
        Long memberId = stompSessions.remove(sessionId);
        if (memberId == null) {
            return;
        }

        if (stompSessions.isConnected(memberId)) {
            log.debug("세션 {} 종료, 회원 {} 의 다른 세션이 살아 있어 유예를 걸지 않는다", sessionId, memberId);
            return;
        }

        roomLeaveGrace.beginFor(memberId);
    }
}
