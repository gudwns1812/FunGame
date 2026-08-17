package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.MemberLocation;
import com.fungame.songquiz.support.config.AppTaskScheduler;
import com.fungame.songquiz.support.error.CoreException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class RoomLeaveGrace {

    static final long GRACE_SECONDS = 15;

    private final GameRoomService gameRoomService;
    private final TaskScheduler taskScheduler;
    private final StompSessions stompSessions;

    private final Map<Long, ScheduledFuture<?>> pendingByMember = new ConcurrentHashMap<>();

    public RoomLeaveGrace(GameRoomService gameRoomService,
                          @AppTaskScheduler TaskScheduler taskScheduler,
                          StompSessions stompSessions) {
        this.gameRoomService = gameRoomService;
        this.taskScheduler = taskScheduler;
        this.stompSessions = stompSessions;
    }

    public void beginFor(Long memberId) {
        if (gameRoomService.findLocationOf(memberId).isInLobby()) {
            return;
        }

        log.debug("회원 {} 의 연결이 모두 끊겼다. {}초 안에 돌아오지 않으면 방에서 내보낸다", memberId, GRACE_SECONDS);

        pendingByMember.compute(memberId, (id, alreadyScheduled) -> {
            cancelWithoutInterrupting(alreadyScheduled);
            return taskScheduler.schedule(
                    () -> evictIfStillGone(memberId),
                    Instant.now().plusSeconds(GRACE_SECONDS));
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

        if (stompSessions.isConnected(memberId)) {
            return;
        }

        MemberLocation location = gameRoomService.findLocationOf(memberId);
        if (location.isInLobby()) {
            return;
        }

        try {
            gameRoomService.leaveRoom(location.roomId(), memberId);
            log.info("{}초 안에 돌아오지 않아 방 {} 에서 회원 {} 을 내보낸다",
                    GRACE_SECONDS, location.roomId(), memberId);
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
