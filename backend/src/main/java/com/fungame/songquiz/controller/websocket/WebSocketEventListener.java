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
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private static final long LEAVE_GRACE_SECONDS = 5;

    private final GameRoomService gameRoomService;
    private final ScheduledExecutorService scheduler;

    private final Map<String, UserSession> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> pendingLeaves = new ConcurrentHashMap<>();

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
            log.warn("Cannot resolve nickname for session {} subscribing to room {}", sessionId, roomId);
            return;
        }

        UserSession userSession = new UserSession(roomId, nickname);
        sessionMap.put(sessionId, userSession);
        cancelPendingLeave(userSession);

        log.info("User {} subscribed to room {}", nickname, roomId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        UserSession userSession = sessionMap.remove(sessionId);
        if (userSession == null) {
            return;
        }

        log.info("User {} disconnected from room {}, leaving in {}s unless reconnected",
                userSession.nickname(), userSession.roomId(), LEAVE_GRACE_SECONDS);

        pendingLeaves.compute(userSession.key(), (key, previous) -> {
            if (previous != null) {
                previous.cancel(false);
            }
            return scheduler.schedule(() -> leaveAfterGrace(userSession), LEAVE_GRACE_SECONDS, TimeUnit.SECONDS);
        });
    }

    private void cancelPendingLeave(UserSession userSession) {
        pendingLeaves.computeIfPresent(userSession.key(), (key, task) -> {
            task.cancel(false);
            log.info("Reconnect detected, cancelled pending leave for {}", userSession.nickname());
            return null;
        });
    }

    private void leaveAfterGrace(UserSession userSession) {
        pendingLeaves.remove(userSession.key());

        if (sessionMap.containsValue(userSession)) {
            return;
        }

        try {
            gameRoomService.leaveRoom(userSession.roomId(), userSession.nickname());
        } catch (CoreException e) {
            log.info("Room {} already gone on disconnect of {}", userSession.roomId(), userSession.nickname());
        } catch (Exception e) {
            log.error("Failed to leave room {} for {}", userSession.roomId(), userSession.nickname(), e);
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
        String key() {
            return roomId + ":" + nickname;
        }
    }
}
