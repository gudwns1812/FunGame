package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.domain.event.GameResultEvent;
import com.fungame.songquiz.domain.event.GameStartEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberPresenceEventListener {

    private final MemberPresenceService memberPresenceService;

    @EventListener
    public void handleGameStart(GameStartEvent event) {
        memberPresenceService.markRoomPlaying(event.roomId());
    }

    @EventListener
    public void handleGameResult(GameResultEvent event) {
        memberPresenceService.markRoomWaiting(event.roomId());
    }
}
