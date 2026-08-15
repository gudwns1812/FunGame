package com.fungame.songquiz.support.sse;

import com.fungame.songquiz.domain.member.MemberConnectionTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    static final Duration CONNECTION_LIFETIME = Duration.ofMinutes(30);

    private static final String CONNECTED_EVENT = "connected";
    private static final String HEARTBEAT_EVENT = "heartbeat";

    private final MemberConnectionTracker memberConnectionTracker;

    private final Map<Long, Map<String, SseConnection>> connectionsByMember = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long memberId) {
        String connectionId = UUID.randomUUID().toString();
        SseConnection connection = new SseConnection(connectionId, new SseEmitter(CONNECTION_LIFETIME.toMillis()));

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

    public void broadcast(String eventName, Object data) {
        broadcastEach(eventName, memberId -> data);
    }

    public void broadcastEach(String eventName, MemberPayload payload) {
        connectionsByMember.forEach((memberId, connections) -> {
            Object data = payload.of(memberId);

            connections.forEach((connectionId, connection) -> {
                if (!connection.send(eventName, data)) {
                    unregister(memberId, connectionId);
                }
            });
        });
    }

    @Scheduled(fixedDelay = 20000)
    public void sendHeartbeat() {
        broadcast(HEARTBEAT_EVENT, "ping");
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
