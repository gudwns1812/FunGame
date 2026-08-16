package com.fungame.songquiz.controller.sse;

import com.fungame.songquiz.domain.GameRoomService;
import com.fungame.songquiz.domain.dto.OnlineMembers;
import com.fungame.songquiz.domain.event.MemberPresenceChangedEvent;
import com.fungame.songquiz.domain.event.RoomChangedEvent;
import com.fungame.songquiz.domain.member.OnlineMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class LobbyNotifyService {

    private static final String ROOM_UPDATE_EVENT = "room-update";
    private static final String PRESENCE_UPDATE_EVENT = "presence-update";

    private final SseService sseService;
    private final GameRoomService gameRoomService;
    private final OnlineMemberService onlineMemberService;

    private final AtomicBoolean hasPendingRoomUpdate = new AtomicBoolean(false);
    private final AtomicBoolean hasPendingPresenceUpdate = new AtomicBoolean(false);

    @Async
    @EventListener
    public void handleRoomChangedEvent(RoomChangedEvent event) {
        hasPendingRoomUpdate.set(true);
    }

    @Async
    @EventListener
    public void handleMemberPresenceChangedEvent(MemberPresenceChangedEvent event) {
        hasPendingPresenceUpdate.set(true);
    }

    @Scheduled(fixedDelay = 500)
    public void processPendingUpdate() {
        if (hasPendingRoomUpdate.compareAndSet(true, false)) {
            sseService.broadcast(ROOM_UPDATE_EVENT, gameRoomService.findAllRooms());
        }

        if (hasPendingPresenceUpdate.compareAndSet(true, false)) {
            OnlineMembers onlineMembers = onlineMemberService.findAllOnline();
            sseService.broadcastEach(PRESENCE_UPDATE_EVENT, onlineMembers::excluding);
        }
    }
}
