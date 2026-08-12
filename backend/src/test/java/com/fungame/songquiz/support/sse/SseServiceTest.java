package com.fungame.songquiz.support.sse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fungame.songquiz.domain.event.MemberPresenceChangedEvent;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SseServiceTest {

    private static final Long MEMBER_ID = 11L;
    private static final Long OTHER_MEMBER_ID = 22L;

    private final SseService sseService = new SseService();
    private final ListAppender<ILoggingEvent> capturedLogs = new ListAppender<>();

    private Logger connectionLogger;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        connectionLogger = (Logger) LoggerFactory.getLogger(SseConnection.class);
        originalLevel = connectionLogger.getLevel();
        connectionLogger.setLevel(Level.DEBUG);
        capturedLogs.start();
        connectionLogger.addAppender(capturedLogs);
    }

    @AfterEach
    void tearDown() {
        connectionLogger.detachAppender(capturedLogs);
        connectionLogger.setLevel(originalLevel);
    }

    @Test
    @DisplayName("구독하면 그 회원의 채널이 열린다.")
    void subscribe() {
        SseEmitter emitter = sseService.subscribe(MEMBER_ID);

        assertThat(emitter).isNotNull();
        assertThat(sseService.isOnline(MEMBER_ID)).isTrue();
        assertThat(sseService.onlineMemberIds()).containsExactly(MEMBER_ID);
    }

    @Test
    @DisplayName("구독하지 않은 회원은 접속 중이 아니다.")
    void offlineWithoutSubscription() {
        sseService.subscribe(MEMBER_ID);

        assertThat(sseService.isOnline(OTHER_MEMBER_ID)).isFalse();
    }

    @Test
    @DisplayName("한 회원이 탭을 여러 개 열어도 하나의 회원으로 센다.")
    void multipleTabsOfSameMember() {
        sseService.subscribe(MEMBER_ID);
        sseService.subscribe(MEMBER_ID);

        assertThat(sseService.onlineMemberIds()).containsExactly(MEMBER_ID);
        assertThat(connectionsOf(MEMBER_ID)).hasSize(2);
    }

    @Test
    @DisplayName("특정 회원에게만 이벤트를 보낸다.")
    void sendOnlyToTargetMember() {
        AtomicInteger targetSends = new AtomicInteger();
        AtomicInteger otherSends = new AtomicInteger();
        putConnection(MEMBER_ID, "target", countingEmitter(targetSends));
        putConnection(OTHER_MEMBER_ID, "other", countingEmitter(otherSends));

        sseService.sendTo(MEMBER_ID, "room-invite", "payload");

        assertThat(targetSends).hasValue(1);
        assertThat(otherSends).hasValue(0);
    }

    @Test
    @DisplayName("접속 중이 아닌 회원에게 보내도 예외가 나지 않는다.")
    void sendToOfflineMemberIsNoop() {
        sseService.sendTo(MEMBER_ID, "room-invite", "payload");

        assertThat(sseService.isOnline(MEMBER_ID)).isFalse();
    }

    @Test
    @DisplayName("방 변경 이벤트가 발생하면 다음 주기에 모든 구독자에게 알린다.")
    void broadcastToEverySubscriber() {
        AtomicInteger firstSubscriberSends = new AtomicInteger();
        AtomicInteger secondSubscriberSends = new AtomicInteger();
        putConnection(MEMBER_ID, "first", countingEmitter(firstSubscriberSends));
        putConnection(OTHER_MEMBER_ID, "second", countingEmitter(secondSubscriberSends));

        sseService.handleRoomChangedEvent(new RoomChangedEvent());
        sseService.processPendingUpdate();

        assertThat(firstSubscriberSends).hasValue(1);
        assertThat(secondSubscriberSends).hasValue(1);
    }

    @Test
    @DisplayName("한 주기에 몰린 방 변경 이벤트는 한 번으로 묶어서 알린다.")
    void aggregateEventsWithinOneCycle() {
        AtomicInteger sends = new AtomicInteger();
        putConnection(MEMBER_ID, "first", countingEmitter(sends));

        sseService.handleRoomChangedEvent(new RoomChangedEvent());
        sseService.handleRoomChangedEvent(new RoomChangedEvent());
        sseService.handleRoomChangedEvent(new RoomChangedEvent());
        sseService.processPendingUpdate();

        assertThat(sends).hasValue(1);
    }

    @Test
    @DisplayName("접속 상태 변경도 다음 주기에 한 번으로 묶어서 알린다.")
    void aggregatePresenceEvents() {
        AtomicInteger sends = new AtomicInteger();
        putConnection(MEMBER_ID, "first", countingEmitter(sends));

        sseService.handleMemberPresenceChangedEvent(new MemberPresenceChangedEvent());
        sseService.handleMemberPresenceChangedEvent(new MemberPresenceChangedEvent());
        sseService.processPendingUpdate();

        assertThat(sends).hasValue(1);
    }

    @Test
    @DisplayName("연결이 끊긴 구독자는 에러 로그 없이 제거한다.")
    void removeDisconnectedSubscriberQuietly() {
        putConnection(MEMBER_ID, "gone", disconnectedEmitter());

        sseService.sendHeartbeat();

        assertThat(sseService.isOnline(MEMBER_ID)).isFalse();
        assertThat(loggedLevels()).doesNotContain(Level.ERROR).contains(Level.DEBUG);
    }

    @Test
    @DisplayName("예상하지 못한 전송 오류는 에러 로그로 남기고 제거한다.")
    void removeBrokenSubscriberWithErrorLog() {
        putConnection(MEMBER_ID, "broken", brokenEmitter());

        sseService.sendHeartbeat();

        assertThat(sseService.isOnline(MEMBER_ID)).isFalse();
        assertThat(loggedLevels()).contains(Level.ERROR);
    }

    @Test
    @DisplayName("한 탭이 끊겨도 남은 탭이 있으면 접속 중이다.")
    void stayOnlineWhileAnyTabAlive() {
        putConnection(MEMBER_ID, "alive", countingEmitter(new AtomicInteger()));
        putConnection(MEMBER_ID, "gone", disconnectedEmitter());

        sseService.sendHeartbeat();

        assertThat(sseService.isOnline(MEMBER_ID)).isTrue();
        assertThat(connectionsOf(MEMBER_ID)).containsOnlyKeys("alive");
    }

    private void putConnection(Long memberId, String connectionId, SseEmitter emitter) {
        connections()
                .computeIfAbsent(memberId, id -> new ConcurrentHashMap<>())
                .put(connectionId, new SseConnection(connectionId, emitter));
    }

    private Map<String, SseConnection> connectionsOf(Long memberId) {
        return connections().getOrDefault(memberId, Map.of());
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Map<String, SseConnection>> connections() {
        return (Map<Long, Map<String, SseConnection>>)
                ReflectionTestUtils.getField(sseService, "connectionsByMember");
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
