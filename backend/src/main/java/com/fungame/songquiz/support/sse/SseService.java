package com.fungame.songquiz.support.sse;

import com.fungame.songquiz.domain.event.MemberPresenceChangedEvent;
import com.fungame.songquiz.domain.event.RoomChangedEvent;
import com.fungame.songquiz.domain.member.MemberConnectionTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 5;
    private static final String CONNECTED_EVENT = "connected";
    private static final String ROOM_UPDATE_EVENT = "room-update";
    private static final String PRESENCE_UPDATE_EVENT = "presence-update";
    private static final String HEARTBEAT_EVENT = "heartbeat";
    private static final String REFRESH_PAYLOAD = "REFRESH";

    private final MemberConnectionTracker memberConnectionTracker;

    private final Map<Long, Map<String, SseConnection>> connectionsByMember = new ConcurrentHashMap<>();
    private final AtomicBoolean hasPendingRoomUpdate = new AtomicBoolean(false);
    private final AtomicBoolean hasPendingPresenceUpdate = new AtomicBoolean(false);

    public SseEmitter subscribe(Long memberId) {
        String connectionId = UUID.randomUUID().toString();
        SseConnection connection = new SseConnection(connectionId, new SseEmitter(DEFAULT_TIMEOUT));

        register(memberId, connectionId, connection);
        connection.send(CONNECTED_EVENT, "Connected");

        return connection.emitter();
    }

    public void sendTo(Long memberId, String eventName, Object data) {
        connectionsOf(memberId).forEach((connectionId, connection) -> {
            if (!connection.send(eventName, data)) {
                unregister(memberId, connectionId);
            }
        });
    }

    @Async
    @EventListener
    public void handleRoomChangedEvent(RoomChangedEvent event) {
        hasPendingRoomUpdate.set(true);
    }

    @Async
    @EventListener
    public void handleMemberPresenceChangedEvent(MemberPresenceChangedEvent event) {
        hasPendingPresenceUpdate.set(true);
    }

    @Scheduled(fixedDelay = 500)
    public void processPendingUpdate() {
        if (hasPendingRoomUpdate.compareAndSet(true, false)) {
            broadcast(ROOM_UPDATE_EVENT, REFRESH_PAYLOAD);
        }

        if (hasPendingPresenceUpdate.compareAndSet(true, false)) {
            broadcast(PRESENCE_UPDATE_EVENT, REFRESH_PAYLOAD);
        }
    }

    @Scheduled(fixedDelay = 20000)
    public void sendHeartbeat() {
        broadcast(HEARTBEAT_EVENT, "ping");
    }

    private void broadcast(String eventName, Object data) {
        connectionsByMember.forEach((memberId, connections) ->
                connections.forEach((connectionId, connection) -> {
                    if (!connection.send(eventName, data)) {
                        unregister(memberId, connectionId);
                    }
                }));
    }

    private void register(Long memberId, String connectionId, SseConnection connection) {
        connectionsByMember
                .computeIfAbsent(memberId, id -> new ConcurrentHashMap<>())
                .put(connectionId, connection);

        connection.emitter().onCompletion(() -> unregister(memberId, connectionId));
        connection.emitter().onTimeout(() -> unregister(memberId, connectionId));
        connection.emitter().onError(error -> unregister(memberId, connectionId));

        memberConnectionTracker.connect(memberId, connectionId);
    }

    private void unregister(Long memberId, String connectionId) {
        connectionsByMember.computeIfPresent(memberId, (id, connections) -> {
            connections.remove(connectionId);
            return connections.isEmpty() ? null : connections;
        });

        memberConnectionTracker.disconnect(memberId, connectionId);
    }

    private Map<String, SseConnection> connectionsOf(Long memberId) {
        return connectionsByMember.getOrDefault(memberId, Map.of());
    }
}
