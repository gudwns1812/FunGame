package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.controller.response.ApiResponse;
import com.fungame.songquiz.domain.invite.RoomInviteCreatedEvent;
import com.fungame.songquiz.domain.member.MemberAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InviteNotifyService {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleRoomInviteCreated(RoomInviteCreatedEvent event) {
        messagingTemplate.convertAndSendToUser(
                MemberAdapter.principalNameOf(event.targetMemberId()),
                StompDestination.INVITE,
                ApiResponse.success(event.notification()));
    }
}
