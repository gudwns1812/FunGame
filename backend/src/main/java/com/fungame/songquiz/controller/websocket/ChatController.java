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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GameRoomManager gameRoomManager;
    private final GameService gameService;

    @MessageMapping("/room/{roomId}/chat")
    public void chat(@DestinationVariable Long roomId, Principal principal, @Payload ChatRequest request) {
        MemberAdapter user = (MemberAdapter) ((Authentication) principal).getPrincipal();
        log.info("Chat in room {}: {} - {}", roomId, user.getNickName(), request.message());
        Object payload = Map.of(
                "type", "CHAT",
                "playerName", user.getNickName(),
                "message", request.message()
        );

        messagingTemplate.convertAndSend("/subscribe/room/" + roomId, ApiResponse.success(payload));

        try {
            gameRoomManager.touch(roomId);
            gameService.processAnswer(roomId, user.getNickName(), request.message());
        } catch (CoreException e) {
            log.info("방없음");
        }
    }

    @MessageMapping("/room/{roomId}/action")
    public void handleAction(@DestinationVariable Long roomId, Principal principal, GameAction action) {
        MemberAdapter user = (MemberAdapter) ((Authentication) principal).getPrincipal();
        log.info("Action in room {}: {} - {}", roomId, user.getNickName(), action);
        gameRoomManager.touch(roomId);
        // 클라이언트에서 보낸 action의 playerName이 실제 헤더와 일치하는지 검증 로직 추가 가능
        gameService.handleAction(roomId, action);
    }
}
