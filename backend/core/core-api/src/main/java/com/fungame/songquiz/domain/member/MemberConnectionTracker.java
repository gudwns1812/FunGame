package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.domain.event.MemberPresenceChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MemberConnectionTracker {

    static final Duration RECONNECT_GRACE = Duration.ofSeconds(20);

    private static final long GRACE_SWEEP_INTERVAL_MS = 1000;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    private final Map<Long, Set<String>> liveConnectionIdsByMember = new ConcurrentHashMap<>();
    private final Map<Long, Instant> offlineDeadlineByMember = new ConcurrentHashMap<>();

    public void connect(Long memberId, String connectionId) {
        boolean wasOnline = isOnline(memberId);

        addLiveConnection(memberId, connectionId);
        offlineDeadlineByMember.remove(memberId);

        if (!wasOnline) {
            announceOnlineChange();
        }
    }

    public void disconnect(Long memberId, String connectionId) {
        removeLiveConnection(memberId, connectionId);

        if (!hasLiveConnection(memberId)) {
            offlineDeadlineByMember.putIfAbsent(memberId, now().plus(RECONNECT_GRACE));
        }
    }

    public boolean hasLiveConnection(Long memberId) {
        return liveConnectionIdsByMember.containsKey(memberId);
    }

    public Set<Long> onlineMemberIds() {
        Set<Long> onlineMemberIds = new HashSet<>(liveConnectionIdsByMember.keySet());

        offlineDeadlineByMember.forEach((memberId, offlineDeadline) -> {
            if (isWithinGrace(offlineDeadline)) {
                onlineMemberIds.add(memberId);
            }
        });

        return Set.copyOf(onlineMemberIds);
    }

    @Scheduled(fixedDelay = GRACE_SWEEP_INTERVAL_MS)
    public void expireReconnectGrace() {
        List<Long> memberIdsOutOfGrace = offlineDeadlineByMember.entrySet().stream()
                .filter(offlineDeadlineOfMember -> !isWithinGrace(offlineDeadlineOfMember.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        if (memberIdsOutOfGrace.isEmpty()) {
            return;
        }

        memberIdsOutOfGrace.forEach(offlineDeadlineByMember::remove);
        announceOnlineChange();
    }

    private void addLiveConnection(Long memberId, String connectionId) {
        liveConnectionIdsByMember.compute(memberId, (id, connectionIds) -> {
            Set<String> liveConnectionIds = connectionIds == null ? ConcurrentHashMap.newKeySet() : connectionIds;
            liveConnectionIds.add(connectionId);
            return liveConnectionIds;
        });
    }

    private void removeLiveConnection(Long memberId, String connectionId) {
        liveConnectionIdsByMember.computeIfPresent(memberId, (id, connectionIds) -> {
            connectionIds.remove(connectionId);
            return connectionIds.isEmpty() ? null : connectionIds;
        });
    }

    private boolean isOnline(Long memberId) {
        return hasLiveConnection(memberId) || isWithinGrace(offlineDeadlineByMember.get(memberId));
    }

    private boolean isWithinGrace(Instant offlineDeadline) {
        return offlineDeadline != null && now().isBefore(offlineDeadline);
    }

    private void announceOnlineChange() {
        applicationEventPublisher.publishEvent(new MemberPresenceChangedEvent());
    }

    private Instant now() {
        return clock.instant();
    }
}
