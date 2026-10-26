package com.fungame.songquiz.api.websocket;

import com.fungame.songquiz.api.controller.request.ChatRequest;
import com.fungame.songquiz.api.controller.request.GameActionRequest;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.domain.room.ChatMessageEvent;
import com.fungame.songquiz.domain.room.GameRoomManager;
import com.fungame.songquiz.domain.session.GameService;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final ApplicationEventPublisher eventPublisher;
    private final GameRoomManager gameRoomManager;
    private final GameService gameService;

    @MessageMapping("/room/{roomId}/chat")
    public void chat(@DestinationVariable Long roomId, @AuthenticationPrincipal MemberAdapter user,
                     @Payload ChatRequest request) {
        eventPublisher.publishEvent(new ChatMessageEvent(roomId, user.getId(), request.message()));

        try {
            gameRoomManager.touch(roomId);
            gameService.processAnswer(roomId, user.getId(), request.message());
        } catch (CoreException e) {
            logRejected("채팅", roomId, e);
        }
    }

    @MessageMapping("/room/{roomId}/action")
    public void handleAction(@DestinationVariable Long roomId, @AuthenticationPrincipal MemberAdapter user,
                             GameActionRequest request) {
        try {
            gameRoomManager.touch(roomId);
            gameService.handleAction(roomId, request.toAction(user.getId()));
        } catch (CoreException e) {
            logRejected("행동", roomId, e);
        }
    }

    private static void logRejected(String what, Long roomId, CoreException e) {
        ErrorType type = e.getType();
        switch (type.getLogLevel()) {
            case WARN -> log.warn("방 {} 의 {} 을 처리하지 못했다. {} : {}", roomId, what, type.getCode(), e.getMessage());
            case ERROR -> log.error("방 {} 의 {} 을 처리하지 못했다. {} : {}", roomId, what, type.getCode(), e.getMessage(), e);
            default -> log.debug("방 {} 의 {} 을 처리하지 못했다. {} : {}", roomId, what, type.getCode(), e.getMessage());
        }
    }
}
