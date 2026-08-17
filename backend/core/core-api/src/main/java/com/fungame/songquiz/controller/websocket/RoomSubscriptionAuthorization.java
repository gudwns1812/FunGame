package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.domain.room.GameRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomSubscriptionAuthorization implements ChannelInterceptor {

    private final GameRoomService gameRoomService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        Long roomId = StompDestination.roomIdOf(accessor.getDestination());
        if (roomId == null) {
            return message;
        }

        Long memberId = StompUser.memberIdOf(accessor.getUser());
        if (memberId != null && gameRoomService.hasPlayer(roomId, memberId)) {
            return message;
        }

        log.warn("방 {} 소속이 아닌 세션 {} 의 구독을 등록하지 않는다: 회원 {}",
                roomId, accessor.getSessionId(), memberId);
        return null;
    }
}
