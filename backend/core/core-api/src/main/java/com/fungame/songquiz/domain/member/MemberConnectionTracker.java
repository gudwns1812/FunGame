package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.storage.redis.MemberPresenceDao;
import com.fungame.songquiz.support.config.InstanceId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MemberConnectionTracker {

    static final Duration RECONNECT_GRACE = Duration.ofSeconds(20);
    static final Duration CONNECTION_LEASE = Duration.ofSeconds(30);

    private static final long GRACE_SWEEP_INTERVAL_MS = 1000;
    private static final long LEASE_RENEW_INTERVAL_MS = 10000;
    private static final String CONNECTION_ID_SEPARATOR = ":";

    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;
    private final MemberPresenceDao memberPresenceDao;
    private final InstanceId instanceId;

    private final Map<Long, Set<String>> myConnectionIdsByMember = new ConcurrentHashMap<>();

    public void connect(Long memberId, String sessionId) {
        String connectionId = connectionIdOf(sessionId);
        addMyConnection(memberId, connectionId);

        Instant now = now();
        boolean wasOnline = memberPresenceDao.connect(memberId, connectionId, now, CONNECTION_LEASE);

        if (!wasOnline) {
            announceOnlineChange();
        }
    }

    public void disconnect(Long memberId, String sessionId) {
        String connectionId = connectionIdOf(sessionId);
        removeMyConnection(memberId, connectionId);

        Instant now = now();
        memberPresenceDao.disconnect(memberId, connectionId, now, now.plus(RECONNECT_GRACE));
    }

    public boolean hasLiveConnection(Long memberId) {
        return memberPresenceDao.hasLiveConnection(memberId, now());
    }

    public int onlineCount() {
        return (int) memberPresenceDao.countOnline(now());
    }

    public Set<Long> onlineMemberIds() {
        return memberPresenceDao.onlineMemberIds(now());
    }

    @Scheduled(fixedDelay = GRACE_SWEEP_INTERVAL_MS)
    public void expireReconnectGrace() {
        if (memberPresenceDao.expireOffline(now())) {
            announceOnlineChange();
        }
    }

    @Scheduled(fixedDelay = LEASE_RENEW_INTERVAL_MS)
    public void renewLeases() {
        Map<Long, Set<String>> snapshot = new HashMap<>();
        myConnectionIdsByMember.forEach((memberId, connectionIds) -> snapshot.put(memberId, Set.copyOf(connectionIds)));

        memberPresenceDao.renew(snapshot, now(), CONNECTION_LEASE);
    }

    private String connectionIdOf(String sessionId) {
        return instanceId.value() + CONNECTION_ID_SEPARATOR + sessionId;
    }

    private void addMyConnection(Long memberId, String connectionId) {
        myConnectionIdsByMember.compute(memberId, (id, connectionIds) -> {
            Set<String> mine = connectionIds == null ? ConcurrentHashMap.newKeySet() : connectionIds;
            mine.add(connectionId);
            return mine;
        });
    }

    private void removeMyConnection(Long memberId, String connectionId) {
        myConnectionIdsByMember.computeIfPresent(memberId, (id, connectionIds) -> {
            connectionIds.remove(connectionId);
            return connectionIds.isEmpty() ? null : connectionIds;
        });
    }

    private void announceOnlineChange() {
        applicationEventPublisher.publishEvent(new MemberPresenceChangedEvent());
    }

    private Instant now() {
        return clock.instant();
    }
}
