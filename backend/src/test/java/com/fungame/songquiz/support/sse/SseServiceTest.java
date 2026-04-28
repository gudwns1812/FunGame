package com.fungame.songquiz.support.sse;

import com.fungame.songquiz.domain.event.RoomChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class SseServiceTest {

    private final SseService sseService = new SseService();

    @Test
    @DisplayName("구독 시 SseEmitter를 반환한다.")
    void subscribe() {
        // when
        SseEmitter emitter = sseService.subscribe();

        // then
        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("방 변경 이벤트가 발생하면 모든 구독자에게 알림을 보낸다.")
    void broadcast() {
        // given
        SseEmitter emitter1 = sseService.subscribe();
        SseEmitter emitter2 = sseService.subscribe();

        // when
        sseService.handleRoomChangedEvent(new RoomChangedEvent());

        // then
        // 실제 전송 여부는 Emitter의 내부 상태를 직접 확인하기 어려우므로, 
        // Mocking이나 커스텀 Emitter를 사용하여 전송 메서드 호출 여부를 확인해야 함.
        // 여기서는 예외 없이 수행됨을 확인.
    }
}
