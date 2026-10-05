package com.fungame.songquiz.api.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisStreamSpreader spreader;

    public void send(String destination, Object payload) {
        spreader.spread(destination, BroadcastMessage.EVERYONE, payload);
    }

    public void sendToUser(String user, String destination, Object payload) {
        spreader.spread(destination, user, payload);
    }

    public void deliverLocally(BroadcastMessage message, Object payload) {
        if (message.isForEveryone()) {
            messagingTemplate.convertAndSend(message.destination(), payload);
            return;
        }

        messagingTemplate.convertAndSendToUser(message.user(), message.destination(), payload);
    }
}
