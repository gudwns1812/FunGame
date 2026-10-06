package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.storage.redis.MemberPresenceDao;
import com.fungame.songquiz.support.config.InstanceId;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MemberConnectionTracker {

    private static final Duration ALIVE_TTL = Duration.ofSeconds(30);
    private static final long HEARTBEAT_INTERVAL_MS = 10000;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final MemberPresenceDao memberPresenceDao;
    private final InstanceId instanceId;

    @PostConstruct
    public void start() {
        memberPresenceDao.markDead(instanceId.value());
        forgetDeadInstances();
        memberPresenceDao.keepAlive(instanceId.value(), ALIVE_TTL);
    }

    @Scheduled(fixedDelay = HEARTBEAT_INTERVAL_MS)
    public void heartbeat() {
        memberPresenceDao.keepAlive(instanceId.value(), ALIVE_TTL);
        forgetDeadInstances();
    }

    public void connect(Long memberId, String sessionId) {
        if (memberPresenceDao.connect(memberId, instanceId.value(), sessionId)) {
            announceOnlineChange();
        }
    }

    public void disconnect(Long memberId, String sessionId) {
        if (memberPresenceDao.disconnect(memberId, instanceId.value(), sessionId)) {
            announceOnlineChange();
        }
    }

    public boolean hasLiveConnection(Long memberId) {
        return memberPresenceDao.hasLiveConnection(memberId);
    }

    public int onlineCount() {
        return (int) memberPresenceDao.countOnline();
    }

    public Set<Long> onlineMemberIds() {
        return memberPresenceDao.onlineMemberIds();
    }

    private void forgetDeadInstances() {
        if (memberPresenceDao.forgetDeadInstances() > 0) {
            announceOnlineChange();
        }
    }

    private void announceOnlineChange() {
        applicationEventPublisher.publishEvent(new MemberPresenceChangedEvent());
    }
}
