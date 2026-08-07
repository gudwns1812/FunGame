package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.domain.GameRoomService;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.support.error.CoreException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private static final String ROOM_DESTINATION_PREFIX = "/subscribe/room/";

    private final GameRoomService gameRoomService;
    private final Map<String, UserSession> sessionMap = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        log.info("Received a new web socket connection");
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        if (destination == null || !destination.startsWith(ROOM_DESTINATION_PREFIX)) {
            return;
        }

        Long roomId = parseRoomId(destination.substring(ROOM_DESTINATION_PREFIX.length()));
        if (roomId == null) {
            return;
        }

        // 핸드셰이크 시점의 인증 정보에서 닉네임을 얻는다.
        // HTTP 세션 속성에는 nickname 이 담기지 않으므로 Principal 을 사용해야 한다.
        String nickname = extractNickname(event.getUser());
        if (nickname == null) {
            log.warn("Cannot resolve nickname for session {} subscribing to room {}", sessionId, roomId);
            return;
        }

        sessionMap.put(sessionId, new UserSession(roomId, nickname));
        log.info("User {} subscribed to room {}", nickname, roomId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        UserSession userSession = sessionMap.remove(sessionId);
        if (userSession == null) {
            return;
        }

        log.info("User {} disconnected from room {}", userSession.nickname(), userSession.roomId());
        try {
            gameRoomService.leaveRoom(userSession.roomId(), userSession.nickname());
        } catch (CoreException e) {
            // 이미 정리된 방이면 무시한다.
            log.info("Room {} already gone on disconnect of {}", userSession.roomId(), userSession.nickname());
        }
    }

    private Long parseRoomId(String rawRoomId) {
        try {
            return Long.parseLong(rawRoomId);
        } catch (NumberFormatException e) {
            log.warn("Invalid room id in subscribe destination: {}", rawRoomId);
            return null;
        }
    }

    private String extractNickname(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof MemberAdapter member) {
            return member.getNickName();
        }

        return null;
    }

    private record UserSession(Long roomId, String nickname) {
    }
}
