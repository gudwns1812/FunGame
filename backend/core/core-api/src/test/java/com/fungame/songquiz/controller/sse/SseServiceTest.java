package com.fungame.songquiz.controller.sse;

import com.fungame.songquiz.domain.member.MemberConnectionTracker;
import com.fungame.songquiz.support.MutableClock;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SseServiceTest {

    private static final Long MEMBER_ID = 11L;
    private static final Long OTHER_MEMBER_ID = 22L;

    private final MemberConnectionTracker memberConnectionTracker = new MemberConnectionTracker(
            event -> {
            },
            new MutableClock(Instant.parse("2026-08-14T00:00:00Z"), ZoneId.of("UTC")));
    private final SseService sseService = new SseService(memberConnectionTracker);
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
        assertThat(connectionsOf(MEMBER_ID)).hasSize(1);
        assertThat(memberConnectionTracker.hasLiveConnection(MEMBER_ID)).isTrue();
    }

    @Test
    @DisplayName("구독한 연결에는 정해진 수명을 준다.")
    void subscribedConnectionHasLifetime() {
        SseEmitter emitter = sseService.subscribe(MEMBER_ID);

        assertThat(emitter.getTimeout()).isEqualTo(SseService.CONNECTION_LIFETIME.toMillis());
    }

    @Test
    @DisplayName("한 회원이 탭을 여러 개 열면 채널도 그만큼 열린다.")
    void multipleTabsOfSameMember() {
        sseService.subscribe(MEMBER_ID);
        sseService.subscribe(MEMBER_ID);

        assertThat(connectionsOf(MEMBER_ID)).hasSize(2);
        assertThat(memberConnectionTracker.onlineMemberIds()).containsExactly(MEMBER_ID);
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

        assertThat(connectionsOf(MEMBER_ID)).isEmpty();
    }

    @Test
    @DisplayName("브로드캐스트는 모든 구독자에게 같은 내용을 보낸다.")
    void broadcastToEverySubscriber() {
        List<Object> firstSubscriberData = new ArrayList<>();
        List<Object> secondSubscriberData = new ArrayList<>();
        putConnection(MEMBER_ID, "first", recordingEmitter(firstSubscriberData));
        putConnection(OTHER_MEMBER_ID, "second", recordingEmitter(secondSubscriberData));

        sseService.broadcast("room-update", List.of("방 하나"));

        assertThat(firstSubscriberData).containsExactly(List.of("방 하나"));
        assertThat(secondSubscriberData).containsExactly(List.of("방 하나"));
    }

    @Test
    @DisplayName("받는 사람마다 다른 내용을 보낼 수 있다.")
    void broadcastDifferentPayloadPerMember() {
        List<Object> firstSubscriberData = new ArrayList<>();
        List<Object> secondSubscriberData = new ArrayList<>();
        putConnection(MEMBER_ID, "first", recordingEmitter(firstSubscriberData));
        putConnection(OTHER_MEMBER_ID, "second", recordingEmitter(secondSubscriberData));

        sseService.broadcastEach("presence-update", List::of);

        assertThat(firstSubscriberData).containsExactly(List.of(MEMBER_ID));
        assertThat(secondSubscriberData).containsExactly(List.of(OTHER_MEMBER_ID));
    }

    @Test
    @DisplayName("한 회원의 여러 탭에는 같은 내용을 한 번씩만 만든다.")
    void buildPayloadOncePerMember() {
        AtomicInteger payloadBuilds = new AtomicInteger();
        putConnection(MEMBER_ID, "first", countingEmitter(new AtomicInteger()));
        putConnection(MEMBER_ID, "second", countingEmitter(new AtomicInteger()));

        sseService.broadcastEach("presence-update", memberId -> payloadBuilds.incrementAndGet());

        assertThat(payloadBuilds).hasValue(1);
    }

    @Test
    @DisplayName("연결이 끊긴 구독자는 에러 로그 없이 제거한다.")
    void removeDisconnectedSubscriberQuietly() {
        putConnection(MEMBER_ID, "gone", disconnectedEmitter());

        sseService.sendHeartbeat();

        assertThat(connectionsOf(MEMBER_ID)).isEmpty();
        assertThat(loggedLevels()).doesNotContain(Level.ERROR).contains(Level.DEBUG);
    }

    @Test
    @DisplayName("예상하지 못한 전송 오류는 에러 로그로 남기고 제거한다.")
    void removeBrokenSubscriberWithErrorLog() {
        putConnection(MEMBER_ID, "broken", brokenEmitter());

        sseService.sendHeartbeat();

        assertThat(connectionsOf(MEMBER_ID)).isEmpty();
        assertThat(loggedLevels()).contains(Level.ERROR);
    }

    @Test
    @DisplayName("한 탭이 끊겨도 남은 탭의 채널은 유지한다.")
    void keepAliveTabWhenAnotherIsGone() {
        putConnection(MEMBER_ID, "alive", countingEmitter(new AtomicInteger()));
        putConnection(MEMBER_ID, "gone", disconnectedEmitter());

        sseService.sendHeartbeat();

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

    private static SseEmitter recordingEmitter(List<Object> receivedData) {
        return new SseEmitter() {
            @Override
            public void send(SseEventBuilder builder) {
                builder.build().stream()
                        .map(DataWithMediaType::getData)
                        .filter(data -> !(data instanceof String))
                        .forEach(receivedData::add);
            }
        };
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
