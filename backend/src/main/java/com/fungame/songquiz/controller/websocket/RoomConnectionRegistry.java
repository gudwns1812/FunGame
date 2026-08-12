package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.domain.GameRoomService;
import com.fungame.songquiz.domain.RoomMember;
import com.fungame.songquiz.domain.RoomPresence;
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
public class RoomConnectionRegistry {

    private static final long LEAVE_GRACE_SECONDS = 15;

    private final GameRoomService gameRoomService;
    private final TaskScheduler taskScheduler;
    private final RoomPresence roomPresence;

    private final Map<String, ScheduledFuture<?>> pendingLeavesByMember = new ConcurrentHashMap<>();

    public RoomConnectionRegistry(GameRoomService gameRoomService,
                                  @AppTaskScheduler TaskScheduler taskScheduler,
                                  RoomPresence roomPresence) {
        this.gameRoomService = gameRoomService;
        this.taskScheduler = taskScheduler;
        this.roomPresence = roomPresence;
    }

    public void connected(String sessionId, RoomMember member) {
        roomPresence.arrive(sessionId, member);
        cancelPendingLeave(member);
        log.info("연결 등록: {} (room {}, session {})", member.nickname(), member.roomId(), sessionId);
    }

    public void disconnected(String sessionId) {
        RoomMember member = roomPresence.depart(sessionId);
        if (member == null) {
            return;
        }

        if (isConnected(member)) {
            log.info("세션 {} 종료, 다른 연결이 살아있어 유예를 예약하지 않는다: {}", sessionId, member.nickname());
            return;
        }

        scheduleLeaveAfterGrace(member);
    }

    public boolean isConnected(RoomMember member) {
        return roomPresence.isConnected(member);
    }

    private void scheduleLeaveAfterGrace(RoomMember member) {
        log.info("연결 종료: {} (room {}), {}초 안에 돌아오지 않으면 이탈 처리",
                member.nickname(), member.roomId(), LEAVE_GRACE_SECONDS);

        pendingLeavesByMember.compute(member.key(), (key, alreadyScheduled) -> {
            cancelWithoutInterrupting(alreadyScheduled);
            return taskScheduler.schedule(
                    () -> leaveIfStillDisconnected(member),
                    Instant.now().plusSeconds(LEAVE_GRACE_SECONDS));
        });
    }

    private void cancelPendingLeave(RoomMember member) {
        pendingLeavesByMember.computeIfPresent(member.key(), (key, scheduled) -> {
            cancelWithoutInterrupting(scheduled);
            log.info("재연결 감지, 이탈 유예 취소: {}", member.nickname());
            return null;
        });
    }

    private void cancelWithoutInterrupting(ScheduledFuture<?> scheduled) {
        if (scheduled != null) {
            scheduled.cancel(false);
        }
    }

    private void leaveIfStillDisconnected(RoomMember member) {
        pendingLeavesByMember.remove(member.key());

        if (isConnected(member)) {
            return;
        }

        try {
            gameRoomService.leaveRoom(member.roomId(), member.nickname(), member.memberId());
        } catch (CoreException e) {
            log.info("이탈 처리 시점에 방 {} 이 이미 없음: {}", member.roomId(), member.nickname());
        } catch (Exception e) {
            log.error("방 {} 에서 {} 이탈 처리 실패", member.roomId(), member.nickname(), e);
        }
    }
}
