package com.fungame.songquiz.api.websocket;

import com.fungame.songquiz.api.controller.response.ApiResponse;
import com.fungame.songquiz.api.controller.response.OnlineMemberResponse;
import com.fungame.songquiz.api.controller.response.RoomResponse;
import com.fungame.songquiz.domain.member.MemberPresenceChangedEvent;
import com.fungame.songquiz.domain.member.MemberProfileCache;
import com.fungame.songquiz.domain.member.OnlineMemberService;
import com.fungame.songquiz.domain.member.OnlineMembers;
import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.RoomChangedEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LobbyNotifier {

    private final StompBroadcaster broadcaster;
    private final GameRoomService gameRoomService;
    private final OnlineMemberService onlineMemberService;
    private final MemberProfileCache memberProfileCache;

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
            broadcaster.send(StompDestination.LOBBY,
                    ApiResponse.success(RoomResponse.listFrom(gameRoomService.findAllRooms(), memberProfileCache)));
        }

        if (hasPendingPresenceUpdate.compareAndSet(true, false)) {
            sendPresenceToEveryone();
        }
    }

    private void sendPresenceToEveryone() {
        OnlineMembers onlineMembers = onlineMemberService.findAllOnline();

        broadcaster.send(StompDestination.PRESENCE,
                ApiResponse.success(OnlineMemberResponse.listFrom(onlineMembers.members())));
    }
}
