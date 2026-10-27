package com.fungame.songquiz.api.websocket;

import com.fungame.songquiz.support.logging.LogContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;

public class StompLogContextInterceptor implements ExecutorChannelInterceptor {

    private final ThreadLocal<LogContext> handling = new ThreadLocal<>();

    @Override
    public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
        handling.set(contextOf(message.getHeaders()));
        return message;
    }

    @Override
    public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler,
                                    Exception ex) {
        LogContext context = handling.get();
        if (context == null) {
            return;
        }

        context.close();
        handling.remove();
    }

    private static LogContext contextOf(MessageHeaders headers) {
        return LogContext.open()
                .put(LogContext.WS_SESSION, SimpMessageHeaderAccessor.getSessionId(headers))
                .put(LogContext.MEMBER_ID, StompUser.memberIdOf(SimpMessageHeaderAccessor.getUser(headers)))
                .put(LogContext.ROOM_ID, StompDestination.roomIdIn(SimpMessageHeaderAccessor.getDestination(headers)));
    }
}
