package com.fungame.songquiz.support.sse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fungame.songquiz.domain.event.RoomChangedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SseServiceTest {

    private final SseService sseService = new SseService();
    private final ListAppender<ILoggingEvent> capturedLogs = new ListAppender<>();

    private Logger sseLogger;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        sseLogger = (Logger) LoggerFactory.getLogger(SseService.class);
        originalLevel = sseLogger.getLevel();
        sseLogger.setLevel(Level.DEBUG);
        capturedLogs.start();
        sseLogger.addAppender(capturedLogs);
    }

    @AfterEach
    void tearDown() {
        sseLogger.detachAppender(capturedLogs);
        sseLogger.setLevel(originalLevel);
    }

    @Test
    @DisplayName("구독 시 SseEmitter를 반환한다.")
    void subscribe() {
        SseEmitter emitter = sseService.subscribe();

        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("방 변경 이벤트가 발생하면 다음 주기에 모든 구독자에게 알린다.")
    void broadcastToEverySubscriber() {
        AtomicInteger firstSubscriberSends = new AtomicInteger();
        AtomicInteger secondSubscriberSends = new AtomicInteger();
        subscribers().put("first", countingEmitter(firstSubscriberSends));
        subscribers().put("second", countingEmitter(secondSubscriberSends));

        sseService.handleRoomChangedEvent(new RoomChangedEvent());
        sseService.processPendingUpdate();

        assertThat(firstSubscriberSends).hasValue(1);
        assertThat(secondSubscriberSends).hasValue(1);
    }

    @Test
    @DisplayName("한 주기에 몰린 방 변경 이벤트는 한 번으로 묶어서 알린다.")
    void aggregateEventsWithinOneCycle() {
        AtomicInteger sends = new AtomicInteger();
        subscribers().put("first", countingEmitter(sends));

        sseService.handleRoomChangedEvent(new RoomChangedEvent());
        sseService.handleRoomChangedEvent(new RoomChangedEvent());
        sseService.handleRoomChangedEvent(new RoomChangedEvent());
        sseService.processPendingUpdate();

        assertThat(sends).hasValue(1);
    }

    @Test
    @DisplayName("연결이 끊긴 구독자는 에러 로그 없이 제거한다.")
    void removeDisconnectedSubscriberQuietly() {
        subscribers().put("gone", disconnectedEmitter());

        sseService.sendHeartbeat();

        assertThat(subscribers()).doesNotContainKey("gone");
        assertThat(loggedLevels()).doesNotContain(Level.ERROR).contains(Level.DEBUG);
    }

    @Test
    @DisplayName("예상하지 못한 전송 오류는 에러 로그로 남기고 제거한다.")
    void removeBrokenSubscriberWithErrorLog() {
        subscribers().put("broken", brokenEmitter());

        sseService.sendHeartbeat();

        assertThat(subscribers()).doesNotContainKey("broken");
        assertThat(loggedLevels()).contains(Level.ERROR);
    }

    @SuppressWarnings("unchecked")
    private Map<String, SseEmitter> subscribers() {
        return (Map<String, SseEmitter>) ReflectionTestUtils.getField(sseService, "emitters");
    }

    private List<Level> loggedLevels() {
        return capturedLogs.list.stream().map(ILoggingEvent::getLevel).toList();
    }

    private static SseEmitter countingEmitter(AtomicInteger sendCount) {
        return new SseEmitter() {
            @Override
            public void send(SseEventBuilder builder) {
                sendCount.incrementAndGet();
            }
        };
    }

    private static SseEmitter disconnectedEmitter() {
        return new SseEmitter() {
            @Override
            public void send(SseEventBuilder builder) throws IOException {
                throw new IOException("Broken pipe");
            }
        };
    }

    private static SseEmitter brokenEmitter() {
        return new SseEmitter() {
            @Override
            public void send(SseEventBuilder builder) {
                throw new IllegalStateException("이벤트 직렬화 실패");
            }
        };
    }
}
