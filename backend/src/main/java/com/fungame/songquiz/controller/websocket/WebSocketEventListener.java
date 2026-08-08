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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    /**
     * 연결이 끊긴 뒤 이탈로 확정하기까지 기다리는 시간.
     * 새로고침은 "종료 → 재연결"로 나타나므로, 이 유예 없이는 새로고침한 사람이 방에서 쫓겨난다.
     */
    private static final long LEAVE_GRACE_SECONDS = 5;

    private final GameRoomService gameRoomService;
    private final ScheduledExecutorService scheduler;

    private final Map<String, UserSession> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> pendingLeaves = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        log.info("Received a new web socket connection");
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        Long roomId = StompDestination.roomIdOf(destination);
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
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        UserSession userSession = sessionMap.remove(sessionId);
        if (userSession == null) {
            return;
        }

        log.info("User {} disconnected from room {}, {}초 뒤 이탈 처리 예정",
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
            log.info("재접속 확인으로 {} 의 이탈 처리를 취소한다", userSession.nickname());
            return null;
        });
    }

    private void leaveAfterGrace(UserSession userSession) {
        pendingLeaves.remove(userSession.key());

        // 취소가 경합에 밀렸을 수 있으므로, 실제로 살아있는 세션이 없는지 마지막으로 확인한다.
        if (hasLiveSession(userSession)) {
            return;
        }

        try {
            gameRoomService.leaveRoom(userSession.roomId(), userSession.nickname());
        } catch (CoreException e) {
            // 이미 정리된 방이면 무시한다.
            log.info("Room {} already gone on disconnect of {}", userSession.roomId(), userSession.nickname());
        } catch (Exception e) {
            log.error("이탈 처리 실패: room {}, player {}", userSession.roomId(), userSession.nickname(), e);
        }
    }

    private boolean hasLiveSession(UserSession userSession) {
        return sessionMap.containsValue(userSession);
    }

    /**
     * 이 클래스의 메서드는 @MessageMapping 핸들러가 아니라 @EventListener 다.
     * @EventListener 는 인자 리졸버를 거치지 않으므로 ChatController 처럼
     * @AuthenticationPrincipal 을 쓸 수 없고, 이벤트가 들고 있는 Principal 에서 직접 꺼내야 한다.
     * (핸드셰이크 때 HTTP 세션 속성에는 nickname 이 담기지 않으므로 세션 속성으로는 얻을 수 없다.)
     */
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
