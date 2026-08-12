package com.fungame.songquiz.support.sse;

import com.fungame.songquiz.domain.event.RoomChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class SseService {
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 5; // 5분
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final AtomicBoolean hasPendingUpdate = new AtomicBoolean(false);

    public SseEmitter subscribe() {
        String id = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        emitters.put(id, emitter);

        emitter.onCompletion(() -> emitters.remove(id));
        emitter.onTimeout(() -> emitters.remove(id));
        emitter.onError((e) -> emitters.remove(id));

        // 초기 연결 메시지 전송 (Nginx 타임아웃 방지 및 브라우저 연결 확인용)
        sendToClient(id, emitter, "Connected", "connected");

        return emitter;
    }

    @Async
    @EventListener
    public void handleRoomChangedEvent(RoomChangedEvent event) {
        // Event Aggregation: 500ms 동안 발생하는 이벤트를 하나로 묶음
        hasPendingUpdate.set(true);
    }

    @Scheduled(fixedDelay = 500)
    public void processPendingUpdate() {
        if (hasPendingUpdate.compareAndSet(true, false)) {
            broadcast("REFRESH", "room-update");
        }
    }

    @Scheduled(fixedDelay = 20000) // 20초 주기 하트비트
    public void sendHeartbeat() {
        broadcast("ping", "heartbeat");
    }

    private void broadcast(Object data, String name) {
        emitters.forEach((id, emitter) -> sendToClient(id, emitter, data, name));
    }

    private void sendToClient(String id, SseEmitter emitter, Object data, String name) {
        try {
            emitter.send(SseEmitter.event()
                    .id(id)
                    .name(name)
                    .data(data));
        } catch (IOException e) {
            log.debug("연결이 끊긴 구독자를 제거한다: {}", id);
            emitters.remove(id);
        } catch (Exception e) {
            log.error("예상하지 못한 SSE 전송 오류로 구독자를 제거한다: {}", id, e);
            emitters.remove(id);
        }
    }
}
