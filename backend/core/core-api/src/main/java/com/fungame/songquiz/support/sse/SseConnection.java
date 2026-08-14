package com.fungame.songquiz.support.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
public class SseConnection {

    private final String id;
    private final SseEmitter emitter;

    public SseConnection(String id, SseEmitter emitter) {
        this.id = id;
        this.emitter = emitter;
    }

    public SseEmitter emitter() {
        return emitter;
    }

    public synchronized boolean send(String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(id)
                    .name(eventName)
                    .data(data));
            return true;
        } catch (IOException e) {
            log.debug("연결이 끊긴 구독자를 제거한다: {}", id);
            return false;
        } catch (Exception e) {
            log.error("예상하지 못한 SSE 전송 오류로 구독자를 제거한다: {}", id, e);
            return false;
        }
    }
}
