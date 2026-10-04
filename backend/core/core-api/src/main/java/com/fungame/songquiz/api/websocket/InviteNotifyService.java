package com.fungame.songquiz.api.websocket;

import com.fungame.songquiz.api.controller.response.ApiResponse;
import com.fungame.songquiz.domain.invite.RoomInviteCreatedEvent;
import com.fungame.songquiz.domain.member.MemberAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InviteNotifyService {

    private final StompBroadcaster broadcaster;

    @EventListener
    public void handleRoomInviteCreated(RoomInviteCreatedEvent event) {
        broadcaster.sendToUser(
                MemberAdapter.principalNameOf(event.targetMemberId()),
                StompDestination.INVITE,
                ApiResponse.success(event.notification()));
    }
}
