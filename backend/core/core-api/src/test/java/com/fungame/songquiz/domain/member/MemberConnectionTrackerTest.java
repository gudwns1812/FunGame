package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.storage.redis.MemberPresenceDao;
import com.fungame.songquiz.storage.redis.RedisTestContainer;
import com.fungame.songquiz.support.MutableClock;
import com.fungame.songquiz.support.config.InstanceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
@Import({RedisTestContainer.class, MemberPresenceDao.class})
class MemberConnectionTrackerTest {

    private static final Long MEMBER_ID = 11L;
    private static final Long OTHER_MEMBER_ID = 22L;
    private static final String FIRST_TAB = "first-tab";
    private static final String SECOND_TAB = "second-tab";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MemberPresenceDao memberPresenceDao;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-08-14T00:00:00Z"), ZoneId.of("UTC"));
    private final List<Object> publishedEvents = new ArrayList<>();
    private final ApplicationEventPublisher applicationEventPublisher = publishedEvents::add;

    private MemberConnectionTracker tracker;
    private MemberConnectionTracker otherInstance;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(redisTemplate.keys(MemberPresenceDao.KEY_PREFIX + "*"));
        tracker = trackerOn("instance-a");
        otherInstance = trackerOn("instance-b");
    }

    private MemberConnectionTracker trackerOn(String instance) {
        return new MemberConnectionTracker(applicationEventPublisher, clock, memberPresenceDao, new InstanceId(instance));
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
    @DisplayName("한 탭이 끊겨도 남은 탭이 있으면 접속 중이다.")
    void stayOnlineWhileAnyTabAlive() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        tracker.connect(MEMBER_ID, SECOND_TAB);

        tracker.disconnect(MEMBER_ID, FIRST_TAB);

        assertThat(tracker.hasLiveConnection(MEMBER_ID)).isTrue();
        assertThat(tracker.onlineMemberIds()).containsExactly(MEMBER_ID);
    }

    @Test
    @DisplayName("마지막 연결이 끊겨도 유예 시간 안에는 접속 중으로 남는다.")
    void stayOnlineWithinReconnectGrace() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        clearPublishedEvents();

        tracker.disconnect(MEMBER_ID, FIRST_TAB);
        tracker.expireReconnectGrace();

        assertThat(tracker.onlineMemberIds()).containsExactly(MEMBER_ID);
        assertThat(presenceChangeCount()).isZero();
    }

    @Test
    @DisplayName("유예 시간 안에 다시 연결하면 접속 상태 변경을 알리지 않는다.")
    void silentReconnectWithinGrace() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        tracker.disconnect(MEMBER_ID, FIRST_TAB);
        clearPublishedEvents();

        clock.plus(MemberConnectionTracker.RECONNECT_GRACE.minusSeconds(1));
        tracker.connect(MEMBER_ID, SECOND_TAB);
        clock.plus(MemberConnectionTracker.RECONNECT_GRACE);
        tracker.expireReconnectGrace();

        assertThat(tracker.onlineMemberIds()).containsExactly(MEMBER_ID);
        assertThat(presenceChangeCount()).isZero();
    }

    @Test
    @DisplayName("유예 시간이 지나면 접속 중에서 빠지고 한 번 알린다.")
    void goOfflineAfterGrace() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        tracker.disconnect(MEMBER_ID, FIRST_TAB);
        clearPublishedEvents();

        clock.plus(MemberConnectionTracker.RECONNECT_GRACE);
        tracker.expireReconnectGrace();
        tracker.expireReconnectGrace();

        assertThat(tracker.onlineMemberIds()).isEmpty();
        assertThat(presenceChangeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 연결이 두 번 끊겨도 유예 시간이 늘어나지 않는다.")
    void doNotExtendGraceOnRepeatedDisconnect() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        tracker.disconnect(MEMBER_ID, FIRST_TAB);

        clock.plus(MemberConnectionTracker.RECONNECT_GRACE.minusSeconds(1));
        tracker.disconnect(MEMBER_ID, FIRST_TAB);
        clock.plus(Duration.ofSeconds(1));
        tracker.expireReconnectGrace();

        assertThat(tracker.onlineMemberIds()).isEmpty();
    }

    @Test
    @DisplayName("유예 중인 회원은 이벤트를 받을 연결이 없다.")
    void noLiveConnectionWithinGrace() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        tracker.disconnect(MEMBER_ID, FIRST_TAB);

        assertThat(tracker.hasLiveConnection(MEMBER_ID)).isFalse();
        assertThat(tracker.onlineMemberIds()).containsExactly(MEMBER_ID);
    }

    @Test
    @DisplayName("유예가 끝난 회원이 다시 연결하면 접속 상태 변경을 알린다.")
    void announceReconnectAfterGrace() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        tracker.disconnect(MEMBER_ID, FIRST_TAB);
        clock.plus(MemberConnectionTracker.RECONNECT_GRACE);
        tracker.expireReconnectGrace();
        clearPublishedEvents();

        tracker.connect(MEMBER_ID, SECOND_TAB);

        assertThat(presenceChangeCount()).isEqualTo(1);
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
    @DisplayName("다른 서버에서 끊긴 회원이 유예 안에 이 서버로 다시 붙으면 알리지 않는다.")
    void silentReconnectAcrossInstances() {
        otherInstance.connect(MEMBER_ID, FIRST_TAB);
        otherInstance.disconnect(MEMBER_ID, FIRST_TAB);
        clearPublishedEvents();

        tracker.connect(MEMBER_ID, FIRST_TAB);

        assertThat(tracker.hasLiveConnection(MEMBER_ID)).isTrue();
        assertThat(presenceChangeCount()).isZero();
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
    @DisplayName("여러 서버가 유예 만료를 함께 정리해도 한 번만 알린다.")
    void announceExpiryOnceAcrossInstances() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        tracker.disconnect(MEMBER_ID, FIRST_TAB);
        clearPublishedEvents();

        clock.plus(MemberConnectionTracker.RECONNECT_GRACE);
        tracker.expireReconnectGrace();
        otherInstance.expireReconnectGrace();

        assertThat(tracker.onlineMemberIds()).isEmpty();
        assertThat(presenceChangeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("리스를 갱신하지 못한 서버의 연결은 리스가 끝나면 오프라인이 된다. 죽은 서버가 회원을 붙잡지 않는다.")
    void connectionsOfDeadInstanceExpire() {
        otherInstance.connect(MEMBER_ID, FIRST_TAB);
        clearPublishedEvents();

        clock.plus(MemberConnectionTracker.CONNECTION_LEASE);
        tracker.expireReconnectGrace();

        assertThat(tracker.hasLiveConnection(MEMBER_ID)).isFalse();
        assertThat(tracker.onlineMemberIds()).isEmpty();
        assertThat(presenceChangeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("살아 있는 서버가 리스를 갱신하면 리스 기간이 지나도 접속 중으로 남는다.")
    void renewedLeaseKeepsConnectionAlive() {
        tracker.connect(MEMBER_ID, FIRST_TAB);

        clock.plus(MemberConnectionTracker.CONNECTION_LEASE.minusSeconds(1));
        tracker.renewLeases();
        clock.plus(MemberConnectionTracker.CONNECTION_LEASE.minusSeconds(1));
        tracker.expireReconnectGrace();

        assertThat(tracker.hasLiveConnection(MEMBER_ID)).isTrue();
        assertThat(tracker.onlineMemberIds()).containsExactly(MEMBER_ID);
    }

    @Test
    @DisplayName("끊긴 연결은 리스 갱신으로 되살아나지 않는다.")
    void disconnectedConnectionIsNotRenewed() {
        tracker.connect(MEMBER_ID, FIRST_TAB);
        tracker.disconnect(MEMBER_ID, FIRST_TAB);

        tracker.renewLeases();

        assertThat(tracker.hasLiveConnection(MEMBER_ID)).isFalse();
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
