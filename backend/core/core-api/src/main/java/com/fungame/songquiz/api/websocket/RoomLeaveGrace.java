package com.fungame.songquiz.api.websocket;

import com.fungame.songquiz.domain.member.MemberConnectionTracker;
import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.MemberLocation;
import com.fungame.songquiz.domain.session.GameTimer;
import com.fungame.songquiz.domain.session.GameTimerHandler;
import com.fungame.songquiz.domain.session.GameTimerTask;
import com.fungame.songquiz.support.error.CoreException;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RoomLeaveGrace implements GameTimerHandler {

    private final GameRoomService gameRoomService;
    private final GameTimer gameTimer;
    private final MemberConnectionTracker memberConnectionTracker;
    private final Duration grace;

    public RoomLeaveGrace(GameRoomService gameRoomService,
                          GameTimer gameTimer,
                          MemberConnectionTracker memberConnectionTracker,
                          @Value("${app.room.leave-grace-seconds:15}") long graceSeconds) {
        this.gameRoomService = gameRoomService;
        this.gameTimer = gameTimer;
        this.memberConnectionTracker = memberConnectionTracker;
        this.grace = Duration.ofSeconds(graceSeconds);
    }

    public void beginFor(Long memberId) {
        log.debug("회원 {} 의 연결이 모두 끊겼다. {}초 안에 돌아오지 않으면 방에서 내보낸다", memberId, grace.toSeconds());
        gameTimer.startAfter(grace, GameTimerTask.leaveRoom(memberId));
    }

    public void cancelFor(Long memberId) {
        gameTimer.cancel(GameTimerTask.leaveRoom(memberId));
    }

    @Override
    public List<GameTimerTask.Kind> timerKinds() {
        return List.of(GameTimerTask.Kind.LEAVE_ROOM);
    }

    @Override
    public void onTimer(GameTimerTask task) {
        evictIfStillGone(task.targetId());
    }

    private void evictIfStillGone(Long memberId) {
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
                    grace.toSeconds(), location.roomId(), memberId);
        } catch (CoreException e) {
            log.info("이탈 처리 시점에 방 {} 이 이미 없다: 회원 {}", location.roomId(), memberId);
        } catch (Exception e) {
            log.error("방 {} 에서 회원 {} 이탈 처리 실패", location.roomId(), memberId, e);
        }
    }
}
