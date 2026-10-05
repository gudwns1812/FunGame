package com.fungame.songquiz.api.websocket;

import com.fungame.songquiz.api.controller.response.ApiResponse;
import com.fungame.songquiz.domain.member.MemberProfiles;
import com.fungame.songquiz.domain.room.ChatMessageEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatNotifier {

    private final StompBroadcaster broadcaster;
    private final MemberProfiles memberProfiles;

    @EventListener
    public void handleChatMessage(ChatMessageEvent event) {
        Map<String, Object> payload = Map.of(
                "type", "CHAT",
                "memberId", event.memberId(),
                "nickname", memberProfiles.of(event.memberId()).nickname(),
                "message", event.message());

        broadcaster.send(StompDestination.room(event.roomId()), ApiResponse.success(payload));
    }
}
