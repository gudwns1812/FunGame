package com.fungame.songquiz.controller.sse;

import com.fungame.songquiz.domain.event.RoomInviteCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InviteNotifyService {

    private static final String INVITE_EVENT = "room-invite";

    private final SseService sseService;

    @EventListener
    public void handleRoomInviteCreated(RoomInviteCreatedEvent event) {
        sseService.sendTo(event.targetMemberId(), INVITE_EVENT, event.notification());
    }
}
