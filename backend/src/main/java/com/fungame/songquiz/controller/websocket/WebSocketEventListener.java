package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.domain.member.MemberAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final RoomConnectionRegistry connectionRegistry;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        Long roomId = StompDestination.roomIdOf(headerAccessor.getDestination());
        if (roomId == null) {
            return;
        }

        String nickname = extractNickname(event.getUser());
        if (nickname == null) {
            log.warn("방 {} 을 구독한 세션 {} 의 닉네임을 확인할 수 없다", roomId, sessionId);
            return;
        }

        connectionRegistry.connected(sessionId, new RoomMember(roomId, nickname));
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        connectionRegistry.disconnected(headerAccessor.getSessionId());
    }

    private String extractNickname(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof MemberAdapter member) {
            return member.getNickName();
        }

        return null;
    }
}
