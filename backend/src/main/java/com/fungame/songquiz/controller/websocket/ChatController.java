package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.domain.GameRoomManager;
import com.fungame.songquiz.domain.GameService;
import java.util.Map;

import com.fungame.songquiz.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GameRoomManager gameRoomManager;
    private final GameService gameService;

    @MessageMapping("/room/{roomId}/chat")
    public void chat(@DestinationVariable Long roomId, @Header("playerName") String playerName, String message) {
        log.info("Chat in room {}: {} - {}", roomId, playerName, message);
        Object payload = Map.of(
                "type", "CHAT",
                "playerName", playerName,
                "message", message
        );
        
        messagingTemplate.convertAndSend("/subscribe/room/" + roomId, ApiResponse.success(payload));
        gameRoomManager.touch(roomId);
        gameService.processAnswer(roomId, playerName, message);
    }
}
