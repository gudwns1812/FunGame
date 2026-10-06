package com.fungame.songquiz.api.websocket;

import com.fungame.songquiz.domain.member.MemberConnectionTracker;
import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.MemberLocation;
import com.fungame.songquiz.support.config.AppTaskScheduler;
import com.fungame.songquiz.support.error.CoreException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RoomLeaveGrace {

    private final GameRoomService gameRoomService;
    private final TaskScheduler taskScheduler;
    private final MemberConnectionTracker memberConnectionTracker;
    private final long graceSeconds;

    private final Map<Long, ScheduledFuture<?>> pendingByMember = new ConcurrentHashMap<>();

    public RoomLeaveGrace(GameRoomService gameRoomService,
                          @AppTaskScheduler TaskScheduler taskScheduler,
                          MemberConnectionTracker memberConnectionTracker,
                          @Value("${app.room.leave-grace-seconds:15}") long graceSeconds) {
        this.gameRoomService = gameRoomService;
        this.taskScheduler = taskScheduler;
        this.memberConnectionTracker = memberConnectionTracker;
        this.graceSeconds = graceSeconds;
    }

    public void beginFor(Long memberId) {
        if (gameRoomService.findLocationOf(memberId).isInLobby()) {
            return;
        }

        log.debug("회원 {} 의 연결이 모두 끊겼다. {}초 안에 돌아오지 않으면 방에서 내보낸다", memberId, graceSeconds);

        pendingByMember.compute(memberId, (id, alreadyScheduled) -> {
            cancelWithoutInterrupting(alreadyScheduled);
            return taskScheduler.schedule(
                    () -> evictIfStillGone(memberId),
                    Instant.now().plusSeconds(graceSeconds));
        });
    }

    public void cancelFor(Long memberId) {
        pendingByMember.computeIfPresent(memberId, (id, scheduled) -> {
            cancelWithoutInterrupting(scheduled);
            log.debug("회원 {} 이 다시 접속해 이탈 유예를 취소한다", memberId);
            return null;
        });
    }

    private void evictIfStillGone(Long memberId) {
        pendingByMember.remove(memberId);

        if (memberConnectionTracker.hasLiveConnection(memberId)) {
            return;
        }

        MemberLocation location = gameRoomService.findLocationOf(memberId);
        if (location.isInLobby()) {
            return;
        }

        try {
            gameRoomService.leaveRoom(location.roomId(), memberId);
            log.info("{}초 안에 돌아오지 않아 방 {} 에서 회원 {} 을 내보낸다",
                    graceSeconds, location.roomId(), memberId);
        } catch (CoreException e) {
            log.info("이탈 처리 시점에 방 {} 이 이미 없다: 회원 {}", location.roomId(), memberId);
        } catch (Exception e) {
            log.error("방 {} 에서 회원 {} 이탈 처리 실패", location.roomId(), memberId, e);
        }
    }

    private void cancelWithoutInterrupting(ScheduledFuture<?> scheduled) {
        if (scheduled != null) {
            scheduled.cancel(false);
        }
    }
}
