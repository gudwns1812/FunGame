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

/**
 * 방 소속은 join / leave API 가 정한다. 구독은 수신 채널일 뿐이므로
 * 여기서는 "이 회원이 이 방 소속인가" 만 확인한다. 구독이 소속을 만들지는 않는다.
 * <p>
 * 소속이 아니면 구독을 등록하지 않고 흘려보낸다. ERROR 프레임으로 연결을 끊으면
 * 로비·초대까지 같이 죽고 재연결 루프에 빠지는데, 클라이언트는 연결이 맺어질 때마다
 * join 뒤에 다시 구독하므로 흘려보내는 편이 스스로 복구된다.
 */
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
