package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.domain.GameRoomService;
import com.fungame.songquiz.support.config.AppTaskScheduler;
import com.fungame.songquiz.support.error.CoreException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * "지금 웹소켓으로 연결되어 있는가"(연결 상태)만 관리한다.
 * <p>
 * "이 방의 플레이어인가"(참가 상태)는 {@link com.fungame.songquiz.domain.GameRoom} 의 몫이고,
 * 이 둘은 다른 속도로 변한다. 연결은 백그라운드 탭·모바일 전환·네트워크 전환으로 초 단위로
 * 깜빡이는 게 정상이지만, 참가 상태는 그렇게 자주 바뀌면 안 된다.
 * <p>
 * 그래서 이 클래스는 연결 종료를 곧바로 이탈로 번역하지 않는다. 유예 시간 안에 돌아오지 않은
 * 경우에만 참가 상태를 건드린다. 즉 <b>연결 종료 → 이탈</b> 번역이 일어나는 유일한 지점이다.
 */
@Slf4j
@Component
public class RoomConnectionRegistry {

    /**
     * 연결이 끊긴 뒤 이 시간 안에 돌아오지 않으면 방에서 내보낸다.
     * <p>
     * 프론트의 재연결 대기(5초)에 SockJS 핸드셰이크와 CONNECT/SUBSCRIBE 왕복을 더한 것보다
     * 넉넉히 커야 한다. 같거나 작으면 재연결이 구조적으로 유예를 이길 수 없다.
     */
    private static final long LEAVE_GRACE_SECONDS = 15;

    private final GameRoomService gameRoomService;
    private final TaskScheduler taskScheduler;

    /** 웹소켓 세션 ID -> 그 세션이 대변하는 사람 */
    private final Map<String, RoomMember> connections = new ConcurrentHashMap<>();

    /** 사람 -> 유예 중인 이탈 처리 */
    private final Map<String, ScheduledFuture<?>> pendingLeaves = new ConcurrentHashMap<>();

    public RoomConnectionRegistry(GameRoomService gameRoomService,
                                  @AppTaskScheduler TaskScheduler taskScheduler) {
        this.gameRoomService = gameRoomService;
        this.taskScheduler = taskScheduler;
    }

    /**
     * 세션이 방 토픽을 구독했다. 재연결이라면 유예 중이던 이탈을 취소한다.
     */
    public void connected(String sessionId, RoomMember member) {
        connections.put(sessionId, member);
        cancelPendingLeave(member);
        log.info("연결 등록: {} (room {}, session {})", member.nickname(), member.roomId(), sessionId);
    }

    /**
     * 세션이 끊겼다. 참가 상태는 아직 건드리지 않고, 유예 시간 뒤에 다시 판단한다.
     */
    public void disconnected(String sessionId) {
        RoomMember member = connections.remove(sessionId);
        if (member == null) {
            return;
        }

        if (isConnected(member)) {
            log.info("세션 {} 종료, 다른 연결이 살아있어 유예를 예약하지 않는다: {}", sessionId, member.nickname());
            return;
        }

        scheduleLeaveAfterGrace(member);
    }

    /**
     * 이 사람을 대변하는 살아있는 세션이 하나라도 있는가.
     */
    public boolean isConnected(RoomMember member) {
        return connections.containsValue(member);
    }

    /**
     * 유예 시간 뒤에 이탈 처리를 예약한다.
     * 이미 예약된 게 있으면(짧은 시간에 두 번 끊긴 경우) 취소하고 새 예약으로 대체한다.
     */
    private void scheduleLeaveAfterGrace(RoomMember member) {
        log.info("연결 종료: {} (room {}), {}초 안에 돌아오지 않으면 이탈 처리",
                member.nickname(), member.roomId(), LEAVE_GRACE_SECONDS);

        pendingLeaves.compute(member.key(), (key, alreadyScheduled) -> {
            cancelQuietly(alreadyScheduled);
            return taskScheduler.schedule(
                    () -> leaveIfStillDisconnected(member),
                    Instant.now().plusSeconds(LEAVE_GRACE_SECONDS));
        });
    }

    private void cancelPendingLeave(RoomMember member) {
        pendingLeaves.computeIfPresent(member.key(), (key, scheduled) -> {
            cancelQuietly(scheduled);
            log.info("재연결 감지, 이탈 유예 취소: {}", member.nickname());
            return null;
        });
    }

    /** 이미 시작된 작업은 중단하지 않는다. 그 경우는 leaveIfStillDisconnected 가 다시 판단한다. */
    private void cancelQuietly(ScheduledFuture<?> scheduled) {
        if (scheduled != null) {
            scheduled.cancel(false);
        }
    }

    /**
     * 유예가 끝난 시점에도 여전히 연결이 없으면 그때 방에서 내보낸다.
     */
    private void leaveIfStillDisconnected(RoomMember member) {
        pendingLeaves.remove(member.key());

        if (isConnected(member)) {
            return;
        }

        try {
            gameRoomService.leaveRoom(member.roomId(), member.nickname());
        } catch (CoreException e) {
            log.info("이탈 처리 시점에 방 {} 이 이미 없음: {}", member.roomId(), member.nickname());
        } catch (Exception e) {
            log.error("방 {} 에서 {} 이탈 처리 실패", member.roomId(), member.nickname(), e);
        }
    }
}
