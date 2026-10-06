package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.storage.redis.MemberPresenceDao;
import com.fungame.songquiz.storage.redis.RedisTestContainer;
import com.fungame.songquiz.support.config.InstanceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
@Import({RedisTestContainer.class, MemberPresenceDao.class})
class MemberConnectionTrackerTest {

    private static final Long MEMBER_ID = 11L;
    private static final Long OTHER_MEMBER_ID = 22L;
    private static final String FIRST_TAB = "first-tab";
    private static final String SECOND_TAB = "second-tab";
    private static final String INSTANCE_A = "instance-a";
    private static final String INSTANCE_B = "instance-b";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MemberPresenceDao memberPresenceDao;

    private final List<Object> publishedEvents = new CopyOnWriteArrayList<>();
    private final ApplicationEventPublisher applicationEventPublisher = publishedEvents::add;

    private MemberConnectionTracker tracker;
    private MemberConnectionTracker otherInstance;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(redisTemplate.keys(MemberPresenceDao.KEY_PREFIX + "*"));
        tracker = startedTrackerOn(INSTANCE_A);
        otherInstance = startedTrackerOn(INSTANCE_B);
    }

    @Test
    @DisplayName("연결하면 접속 중이 되고 접속 상태 변경을 알린다.")
    void connect() {
        tracker.connect(MEMBER_ID, FIRST_TAB);

        assertThat(tracker.onlineMemberIds()).containsExactly(MEMBER_ID);
        assertThat(tracker.hasLiveConnection(MEMBER_ID)).isTrue();
        assertThat(presenceChangeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("연결하지 않은 회원은 접속 중이 아니다.")
    void offlineWithoutConnection() {
        tracker.connect(MEMBER_ID, FIRST_TAB);

        assertThat(tracker.onlineMemberIds()).doesNotContain(OTHER_MEMBER_ID);
        assertThat(tracker.hasLiveConnection(OTHER_MEMBER_ID)).isFalse();
    }

    @Test
    @DisplayName("한 회원이 탭을 여러 개 열어도 한 번만 알린다.")
    void announceOnceForMultipleTabs() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        tracker.connect(MEMBER_ID, SECOND_TAB);

        assertThat(tracker.onlineMemberIds()).containsExactly(MEMBER_ID);
        assertThat(presenceChangeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("한 탭이 끊겨도 남은 탭이 있으면 접속 중이고 알리지 않는다.")
    void stayOnlineWhileAnyTabAlive() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        tracker.connect(MEMBER_ID, SECOND_TAB);
        clearPublishedEvents();

        tracker.disconnect(MEMBER_ID, FIRST_TAB);

        assertThat(tracker.hasLiveConnection(MEMBER_ID)).isTrue();
        assertThat(tracker.onlineMemberIds()).containsExactly(MEMBER_ID);
        assertThat(presenceChangeCount()).isZero();
    }

    @Test
    @DisplayName("마지막 연결이 끊기면 유예 없이 바로 오프라인이 되고 한 번 알린다.")
    void goOfflineRightAfterLastDisconnect() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        clearPublishedEvents();

        tracker.disconnect(MEMBER_ID, FIRST_TAB);

        assertThat(tracker.hasLiveConnection(MEMBER_ID)).isFalse();
        assertThat(tracker.onlineMemberIds()).isEmpty();
        assertThat(presenceChangeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 연결이 두 번 끊겨도 한 번만 알린다.")
    void announceOnceOnRepeatedDisconnect() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        clearPublishedEvents();

        tracker.disconnect(MEMBER_ID, FIRST_TAB);
        tracker.disconnect(MEMBER_ID, FIRST_TAB);

        assertThat(presenceChangeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("끊겼다가 다시 연결하면 다시 접속 중이 되고 알린다.")
    void announceReconnect() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        tracker.disconnect(MEMBER_ID, FIRST_TAB);
        clearPublishedEvents();

        tracker.connect(MEMBER_ID, SECOND_TAB);

        assertThat(tracker.onlineMemberIds()).containsExactly(MEMBER_ID);
        assertThat(presenceChangeCount()).isEqualTo(1);
    }

    @RepeatedTest(30)
    @DisplayName("두 서버에서 같은 회원의 마지막 연결 둘이 동시에 끊겨도 오프라인이 되고 한 번만 알린다.")
    void concurrentLastDisconnectsAcrossInstances() throws Exception {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        otherInstance.connect(MEMBER_ID, SECOND_TAB);
        clearPublishedEvents();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> first = executor.submit(() -> disconnectTogether(tracker, FIRST_TAB, ready, go));
            Future<?> second = executor.submit(() -> disconnectTogether(otherInstance, SECOND_TAB, ready, go));
            ready.await();
            go.countDown();
            first.get();
            second.get();
        }

        assertThat(tracker.onlineMemberIds()).isEmpty();
        assertThat(presenceChangeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("끊긴 연결은 서버 생존 신호를 보낸 뒤에도 살아나지 않는다.")
    void disconnectedConnectionStaysGoneAfterHeartbeat() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        tracker.disconnect(MEMBER_ID, FIRST_TAB);

        tracker.heartbeat();
        otherInstance.heartbeat();

        assertThat(tracker.hasLiveConnection(MEMBER_ID)).isFalse();
        assertThat(tracker.onlineMemberIds()).isEmpty();
    }

    @Test
    @DisplayName("다른 서버에 붙은 회원도 접속 중으로 보인다.")
    void seeMembersConnectedToAnotherInstance() {
        otherInstance.connect(MEMBER_ID, FIRST_TAB);

        assertThat(tracker.hasLiveConnection(MEMBER_ID)).isTrue();
        assertThat(tracker.onlineMemberIds()).containsExactly(MEMBER_ID);
        assertThat(tracker.onlineCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 세션 id 라도 서버가 다르면 다른 연결이다. 한쪽이 끊겨도 다른 쪽은 살아 있다.")
    void sameSessionIdOnDifferentInstancesAreDifferentConnections() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        otherInstance.connect(MEMBER_ID, FIRST_TAB);

        tracker.disconnect(MEMBER_ID, FIRST_TAB);

        assertThat(tracker.hasLiveConnection(MEMBER_ID)).isTrue();
    }

    @Test
    @DisplayName("생존 신호가 끊긴 서버의 연결은 정리되기 전이라도 살아 있는 연결로 치지 않는다.")
    void connectionOfDeadInstanceIsNotLive() {
        otherInstance.connect(MEMBER_ID, FIRST_TAB);

        kill(INSTANCE_B);

        assertThat(tracker.hasLiveConnection(MEMBER_ID)).isFalse();
    }

    @Test
    @DisplayName("죽은 서버에서 다른 서버로 옮겨 붙은 회원이 거기서 끊기면 살아 있는 연결이 없다.")
    void memberMovedFromDeadInstanceHasNoLiveConnectionAfterDisconnect() {
        otherInstance.connect(MEMBER_ID, FIRST_TAB);
        kill(INSTANCE_B);

        tracker.connect(MEMBER_ID, SECOND_TAB);
        tracker.disconnect(MEMBER_ID, SECOND_TAB);

        assertThat(tracker.hasLiveConnection(MEMBER_ID)).isFalse();
    }

    @Test
    @DisplayName("살아 있는 서버가 죽은 서버의 연결을 치우고, 여러 서버가 함께 치워도 한 번만 알린다.")
    void sweepDeadInstanceOnce() {
        otherInstance.connect(MEMBER_ID, FIRST_TAB);
        kill(INSTANCE_B);
        clearPublishedEvents();

        tracker.heartbeat();
        startedTrackerOn("instance-c").heartbeat();

        assertThat(tracker.onlineMemberIds()).isEmpty();
        assertThat(presenceChangeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("죽은 서버를 치우면 그 서버에만 붙어 있던 회원을 오프라인이 된 회원으로 알린다. 끊김 이벤트가 없어도 방에서 내보낼 수 있다.")
    void announceMembersOfDeadInstanceWentOffline() {
        otherInstance.connect(MEMBER_ID, FIRST_TAB);
        otherInstance.connect(OTHER_MEMBER_ID, FIRST_TAB);
        tracker.connect(OTHER_MEMBER_ID, SECOND_TAB);
        kill(INSTANCE_B);
        clearPublishedEvents();

        tracker.heartbeat();

        assertThat(wentOfflineMemberIds()).containsExactly(MEMBER_ID);
    }

    @Test
    @DisplayName("같은 서버 id 로 다시 뜨며 이전 연결을 치울 때도 오프라인이 된 회원을 알린다.")
    void announceMembersWentOfflineOnRestart() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        clearPublishedEvents();

        startedTrackerOn(INSTANCE_A);

        assertThat(wentOfflineMemberIds()).containsExactly(MEMBER_ID);
    }

    @Test
    @DisplayName("치울 죽은 서버가 없으면 오프라인 알림을 내지 않는다.")
    void noWentOfflineEventWithoutDeadInstance() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        clearPublishedEvents();

        tracker.heartbeat();

        assertThat(publishedEvents).noneMatch(MembersWentOfflineEvent.class::isInstance);
    }

    @Test
    @DisplayName("죽은 서버를 치워도 다른 서버에 연결이 남은 회원은 접속 중이고 알리지 않는다.")
    void sweepKeepsMemberConnectedElsewhere() {
        otherInstance.connect(MEMBER_ID, FIRST_TAB);
        tracker.connect(MEMBER_ID, SECOND_TAB);
        kill(INSTANCE_B);
        clearPublishedEvents();

        tracker.heartbeat();

        assertThat(tracker.onlineMemberIds()).containsExactly(MEMBER_ID);
        assertThat(tracker.hasLiveConnection(MEMBER_ID)).isTrue();
        assertThat(presenceChangeCount()).isZero();
    }

    @Test
    @DisplayName("살아 있는 서버의 연결은 생존 신호가 오가도 치우지 않는다.")
    void heartbeatKeepsLiveInstances() {
        otherInstance.connect(MEMBER_ID, FIRST_TAB);

        tracker.heartbeat();
        otherInstance.heartbeat();

        assertThat(tracker.onlineMemberIds()).containsExactly(MEMBER_ID);
        assertThat(tracker.hasLiveConnection(MEMBER_ID)).isTrue();
    }

    @Test
    @DisplayName("같은 서버 id 로 다시 뜨면 이전 프로세스의 연결을 치우고 알린다.")
    void restartWithSameIdClearsPreviousConnections() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        clearPublishedEvents();

        startedTrackerOn(INSTANCE_A);

        assertThat(tracker.hasLiveConnection(MEMBER_ID)).isFalse();
        assertThat(tracker.onlineMemberIds()).isEmpty();
        assertThat(presenceChangeCount()).isEqualTo(1);
    }

    private MemberConnectionTracker startedTrackerOn(String instance) {
        MemberConnectionTracker started =
                new MemberConnectionTracker(applicationEventPublisher, memberPresenceDao, new InstanceId(instance));
        started.start();

        return started;
    }

    private void kill(String instance) {
        memberPresenceDao.markDead(instance);
    }

    private void disconnectTogether(MemberConnectionTracker instance, String sessionId,
                                    CountDownLatch ready, CountDownLatch go) {
        ready.countDown();
        try {
            go.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        instance.disconnect(MEMBER_ID, sessionId);
    }

    private List<Long> wentOfflineMemberIds() {
        return publishedEvents.stream()
                .filter(MembersWentOfflineEvent.class::isInstance)
                .map(MembersWentOfflineEvent.class::cast)
                .flatMap(event -> event.memberIds().stream())
                .toList();
    }

    private long presenceChangeCount() {
        return publishedEvents.stream()
                .filter(MemberPresenceChangedEvent.class::isInstance)
                .count();
    }

    private void clearPublishedEvents() {
        publishedEvents.clear();
    }
}
