package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.domain.GameRoomService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

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

        if (destination != null && destination.startsWith("/subscribe/room/")) {
            String roomId = destination.replace("/subscribe/room/", "");
            String nickname = (String) headerAccessor.getSessionAttributes().get("nickname");

            if (nickname != null) {
                sessionMap.put(sessionId, new UserSession(Long.parseLong(roomId), nickname));
                log.info("User {} subscribed to room {}", nickname, roomId);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        UserSession userSession = sessionMap.remove(sessionId);
        if (userSession != null) {
            log.info("User {} disconnected from room {}", userSession.nickname(), userSession.roomId());
            gameRoomService.leaveRoom(userSession.roomId(), userSession.nickname());
        }
    }

    private record UserSession(Long roomId, String nickname) {
    }
}
