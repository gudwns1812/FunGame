package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.controller.response.ApiResponse;
import com.fungame.songquiz.controller.response.OnlineMemberResponse;
import com.fungame.songquiz.controller.response.RoomResponse;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.member.MemberPresenceChangedEvent;
import com.fungame.songquiz.domain.member.OnlineMemberService;
import com.fungame.songquiz.domain.member.OnlineMembers;
import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.RoomChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class LobbyNotifyService {

    private final SimpMessagingTemplate messagingTemplate;
    private final GameRoomService gameRoomService;
    private final OnlineMemberService onlineMemberService;
    private final StompSessions stompSessions;

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
            messagingTemplate.convertAndSend(StompDestination.LOBBY,
                    ApiResponse.success(RoomResponse.listFrom(gameRoomService.findAllRooms())));
        }

        if (hasPendingPresenceUpdate.compareAndSet(true, false)) {
            sendPresenceToEveryone();
        }
    }

    /** 접속자 목록에서 받는 사람 자신은 빠지므로 사람마다 내용이 다르다 */
    private void sendPresenceToEveryone() {
        OnlineMembers onlineMembers = onlineMemberService.findAllOnline();

        stompSessions.connectedMemberIds().forEach(viewerId ->
                messagingTemplate.convertAndSendToUser(
                        MemberAdapter.principalNameOf(viewerId),
                        StompDestination.PRESENCE,
                        ApiResponse.success(OnlineMemberResponse.listFrom(onlineMembers.excluding(viewerId)))));
    }
}
