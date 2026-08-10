package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.domain.GameRoomService;
import com.fungame.songquiz.support.error.CoreException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    private final GameRoomService gameRoomService;
    private final ScheduledExecutorService scheduler;
    private final long leaveGraceSeconds;

    /** 웹소켓 세션 ID -> 그 세션이 대변하는 사람 */
    private final Map<String, RoomMember> connections = new ConcurrentHashMap<>();

    /** 사람 -> 유예 중인 이탈 처리 */
    private final Map<String, ScheduledFuture<?>> pendingLeaves = new ConcurrentHashMap<>();

    public RoomConnectionRegistry(GameRoomService gameRoomService,
                                  ScheduledExecutorService scheduler,
                                  @Value("${game.leave-grace-seconds:15}") long leaveGraceSeconds) {
        this.gameRoomService = gameRoomService;
        this.scheduler = scheduler;
        this.leaveGraceSeconds = leaveGraceSeconds;
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
            // 다른 세션으로 이미 돌아와 있다. 이탈 판단 자체가 필요 없다.
            log.info("세션 {} 종료, 다른 연결이 살아있어 유예를 예약하지 않는다: {}", sessionId, member.nickname());
            return;
        }

        log.info("연결 종료: {} (room {}), {}초 안에 돌아오지 않으면 이탈 처리",
                member.nickname(), member.roomId(), leaveGraceSeconds);

        pendingLeaves.compute(member.key(), (key, previous) -> {
            if (previous != null) {
                previous.cancel(false);
            }
            return scheduler.schedule(() -> leaveAfterGrace(member), leaveGraceSeconds, TimeUnit.SECONDS);
        });
    }

    /**
     * 이 사람을 대변하는 살아있는 세션이 하나라도 있는가.
     */
    public boolean isConnected(RoomMember member) {
        return connections.containsValue(member);
    }

    private void cancelPendingLeave(RoomMember member) {
        pendingLeaves.computeIfPresent(member.key(), (key, task) -> {
            task.cancel(false);
            log.info("재연결 감지, 이탈 유예 취소: {}", member.nickname());
            return null;
        });
    }

    private void leaveAfterGrace(RoomMember member) {
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
