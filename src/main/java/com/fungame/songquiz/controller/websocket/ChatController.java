package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.domain.GameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@Slf4j
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GameService gameService;

    public ChatController(SimpMessagingTemplate messagingTemplate, GameService gameService) {
        this.messagingTemplate = messagingTemplate;
        this.gameService = gameService;
    }

    @MessageMapping("/room/{roomId}/chat")
    public void chat(@DestinationVariable String roomId, @Header("nickname") String nickname, String message) {
        log.info("Chat in room {}: {} - {}", roomId, nickname, message);
        Object payload = Map.of(
                "type", "CHAT",
                "nickname", nickname,
                "message", message
        );
        messagingTemplate.convertAndSend("/subscribe/room/" + roomId, payload);

        gameService.processAnswer(roomId, nickname, message);
    }
}
