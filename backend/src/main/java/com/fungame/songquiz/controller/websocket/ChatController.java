package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.controller.request.ChatRequest;
import com.fungame.songquiz.domain.GameAction;
import com.fungame.songquiz.domain.GameRoomManager;
import com.fungame.songquiz.domain.GameService;
import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GameRoomManager gameRoomManager;
    private final GameService gameService;

    @MessageMapping("/room/{roomId}/chat")
    public void chat(@DestinationVariable Long roomId, @AuthenticationPrincipal MemberAdapter user,
                     @Payload ChatRequest request) {
        Object payload = Map.of(
                "type", "CHAT",
                "playerName", user.getNickName(),
                "message", request.message()
        );

        messagingTemplate.convertAndSend(StompDestination.room(roomId), ApiResponse.success(payload));

        try {
            gameRoomManager.touch(roomId);
            gameService.processAnswer(roomId, user.getNickName(), request.message());
        } catch (CoreException e) {
            log.info("Chat for missing room {}: {}", roomId, e.getMessage());
        }
    }

    @MessageMapping("/room/{roomId}/action")
    public void handleAction(@DestinationVariable Long roomId, @AuthenticationPrincipal MemberAdapter user,
                            GameAction action) {
        try {
            gameRoomManager.touch(roomId);
            gameService.handleAction(roomId, action);
        } catch (CoreException e) {
            log.info("Action for missing room {}: {}", roomId, e.getMessage());
        }
    }
}
