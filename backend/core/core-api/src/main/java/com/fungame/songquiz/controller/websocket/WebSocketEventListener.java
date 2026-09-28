package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.domain.member.DailyActiveMembers;
import com.fungame.songquiz.domain.member.MemberConnectionTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final StompSessions stompSessions;
    private final RoomLeaveGrace roomLeaveGrace;
    private final MemberConnectionTracker memberConnectionTracker;
    private final DailyActiveMembers dailyActiveMembers;

    @EventListener
    public void handleConnected(SessionConnectedEvent event) {
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
        Long memberId = StompUser.memberIdOf(event.getUser());
        if (memberId == null) {
            log.warn("세션 {} 의 회원 정보를 확인할 수 없다", sessionId);
            return;
        }

        stompSessions.add(sessionId, memberId);
        memberConnectionTracker.connect(memberId, sessionId);
        roomLeaveGrace.cancelFor(memberId);
        // 활동일은 로그인이 아니라 여기서 남긴다. 세션이 유지되면 며칠씩 붙어 있어도 로그인은 한 번뿐이다.
        dailyActiveMembers.record(memberId);
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

        memberConnectionTracker.disconnect(memberId, sessionId);

        if (stompSessions.isConnected(memberId)) {
            log.debug("세션 {} 종료, 회원 {} 의 다른 세션이 살아 있어 유예를 걸지 않는다", sessionId, memberId);
            return;
        }

        roomLeaveGrace.beginFor(memberId);
    }
}
