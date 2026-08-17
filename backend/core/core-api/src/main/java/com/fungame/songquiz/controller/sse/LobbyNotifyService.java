package com.fungame.songquiz.controller.sse;

import com.fungame.songquiz.domain.member.MemberPresenceChangedEvent;
import com.fungame.songquiz.domain.member.OnlineMemberService;
import com.fungame.songquiz.domain.member.OnlineMembers;
import com.fungame.songquiz.controller.response.OnlineMemberResponse;
import com.fungame.songquiz.controller.response.RoomResponse;
import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.RoomChangedEvent;
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
        hasPendingPresenceUpdate.set(true);
    }

    @Async
    @EventListener
    public void handleMemberPresenceChangedEvent(MemberPresenceChangedEvent event) {
        hasPendingPresenceUpdate.set(true);
    }

    @Scheduled(fixedDelay = 500)
    public void processPendingUpdate() {
        if (hasPendingRoomUpdate.compareAndSet(true, false)) {
            sseService.broadcast(ROOM_UPDATE_EVENT, RoomResponse.listFrom(gameRoomService.findAllRooms()));
        }

        if (hasPendingPresenceUpdate.compareAndSet(true, false)) {
            OnlineMembers onlineMembers = onlineMemberService.findAllOnline();
            sseService.broadcastEach(PRESENCE_UPDATE_EVENT,
                    viewerId -> OnlineMemberResponse.listFrom(onlineMembers.excluding(viewerId)));
        }
    }
}
