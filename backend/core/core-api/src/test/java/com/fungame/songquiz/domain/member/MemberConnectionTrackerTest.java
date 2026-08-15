package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.domain.event.MemberPresenceChangedEvent;
import com.fungame.songquiz.support.MutableClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MemberConnectionTrackerTest {

    private static final Long MEMBER_ID = 11L;
    private static final Long OTHER_MEMBER_ID = 22L;
    private static final String FIRST_TAB = "first-tab";
    private static final String SECOND_TAB = "second-tab";

    private final MutableClock clock = new MutableClock(Instant.parse("2026-08-14T00:00:00Z"), ZoneId.of("UTC"));
    private final List<Object> publishedEvents = new ArrayList<>();
    private final ApplicationEventPublisher applicationEventPublisher = publishedEvents::add;

    private final MemberConnectionTracker tracker =
            new MemberConnectionTracker(applicationEventPublisher, clock);

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

    private long presenceChangeCount() {
        return publishedEvents.stream()
                .filter(MemberPresenceChangedEvent.class::isInstance)
                .count();
    }

    private void clearPublishedEvents() {
        publishedEvents.clear();
    }
}
