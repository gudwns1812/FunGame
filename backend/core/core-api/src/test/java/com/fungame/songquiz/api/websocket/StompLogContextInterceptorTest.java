package com.fungame.songquiz.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.fungame.songquiz.support.StompMessages;
import com.fungame.songquiz.support.logging.LogContext;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

class StompLogContextInterceptorTest {

    private static final String SESSION_ID = "ab12cd34";
    private static final Long MEMBER_ID = 17L;
    private static final Long ROOM_ID = 42L;
    private static final MessageHandler NO_HANDLER = message -> { };

    private final StompLogContextInterceptor interceptor = new StompLogContextInterceptor();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("방으로 보낸 메시지를 처리하는 동안 세션 · 회원 · 방이 보인다.")
    void a_room_message_carries_session_member_and_room() {
        handle(StompMessages.send(SESSION_ID, "/app/room/" + ROOM_ID + "/chat", StompMessages.loggedIn(MEMBER_ID)));

        assertThat(MDC.getCopyOfContextMap()).containsExactlyInAnyOrderEntriesOf(Map.of(
                LogContext.WS_SESSION, SESSION_ID,
                LogContext.MEMBER_ID, String.valueOf(MEMBER_ID),
                LogContext.ROOM_ID, String.valueOf(ROOM_ID)));
    }

    @Test
    @DisplayName("방 토픽 구독에도 방이 붙는다.")
    void a_room_subscription_carries_the_room() {
        handle(StompMessages.subscribe(SESSION_ID, StompDestination.room(ROOM_ID), StompMessages.loggedIn(MEMBER_ID)));

        assertThat(MDC.get(LogContext.ROOM_ID)).isEqualTo(String.valueOf(ROOM_ID));
    }

    @Test
    @DisplayName("방과 상관없는 목적지에는 방을 붙이지 않는다. 회원 정보가 없으면 회원도 붙이지 않는다.")
    void a_message_outside_rooms_carries_only_the_session() {
        handle(StompMessages.subscribe(SESSION_ID, StompDestination.LOBBY, null));

        assertThat(MDC.getCopyOfContextMap()).containsExactly(Map.entry(LogContext.WS_SESSION, SESSION_ID));
    }

    @Test
    @DisplayName("목적지가 없는 프레임에도 세션은 붙는다.")
    void a_frame_without_destination_carries_the_session() {
        handle(StompMessages.session(SESSION_ID));

        assertThat(MDC.getCopyOfContextMap()).containsExactly(Map.entry(LogContext.WS_SESSION, SESSION_ID));
    }

    @Test
    @DisplayName("방 id 가 숫자가 아니면 방을 붙이지 않는다.")
    void a_malformed_room_id_is_left_out() {
        handle(StompMessages.send(SESSION_ID, "/app/room/abc/chat", StompMessages.loggedIn(MEMBER_ID)));

        assertThat(MDC.get(LogContext.ROOM_ID)).isNull();
    }

    @Test
    @DisplayName("처리가 끝나면 문맥을 지운다. 처리 중 예외가 나도 마찬가지다.")
    void the_context_is_removed_after_handling() {
        Message<byte[]> message =
                StompMessages.send(SESSION_ID, "/app/room/" + ROOM_ID + "/chat", StompMessages.loggedIn(MEMBER_ID));
        handle(message);

        interceptor.afterMessageHandled(message, null, NO_HANDLER, new IllegalStateException("처리 실패"));

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    private void handle(Message<?> message) {
        interceptor.beforeHandle(message, null, NO_HANDLER);
    }
}
