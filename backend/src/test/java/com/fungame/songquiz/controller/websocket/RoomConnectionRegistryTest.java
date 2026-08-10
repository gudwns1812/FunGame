package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.domain.GameRoomService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoomConnectionRegistryTest {

    private static final Long ROOM_ID = 1L;
    private static final String NICKNAME = "참가자";
    private static final long GRACE_SECONDS = 1;

    @Mock
    GameRoomService gameRoomService;

    ScheduledExecutorService scheduler;
    RoomConnectionRegistry registry;

    @BeforeEach
    void setUp() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        registry = new RoomConnectionRegistry(gameRoomService, scheduler, GRACE_SECONDS);
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdownNow();
    }

    @Test
    void 연결이_끊겨도_유예_시간_안에는_방에서_내보내지_않는다() {
        // given
        registry.connected("session-1", new RoomMember(ROOM_ID, NICKNAME));

        // when
        registry.disconnected("session-1");

        // then: 끊긴 즉시 이탈 처리하지 않는다 (연결 상태 != 참가 상태)
        verify(gameRoomService, never()).leaveRoom(ROOM_ID, NICKNAME);
        assertThat(registry.isConnected(new RoomMember(ROOM_ID, NICKNAME))).isFalse();
    }

    @Test
    void 유예_시간_안에_재연결하면_방에_그대로_남는다() {
        // given
        registry.connected("session-1", new RoomMember(ROOM_ID, NICKNAME));
        registry.disconnected("session-1");

        // when: 새 세션으로 재연결
        registry.connected("session-2", new RoomMember(ROOM_ID, NICKNAME));

        // then
        assertThat(registry.isConnected(new RoomMember(ROOM_ID, NICKNAME))).isTrue();
        await().during(GRACE_SECONDS * 2, java.util.concurrent.TimeUnit.SECONDS)
                .atMost(GRACE_SECONDS * 3, java.util.concurrent.TimeUnit.SECONDS)
                .untilAsserted(() -> verify(gameRoomService, never()).leaveRoom(ROOM_ID, NICKNAME));
    }

    @Test
    void 유예_시간_안에_돌아오지_않으면_그때_방에서_내보낸다() {
        // given
        registry.connected("session-1", new RoomMember(ROOM_ID, NICKNAME));

        // when
        registry.disconnected("session-1");

        // then
        await().atMost(GRACE_SECONDS * 3, java.util.concurrent.TimeUnit.SECONDS)
                .untilAsserted(() -> verify(gameRoomService).leaveRoom(ROOM_ID, NICKNAME));
    }

    @Test
    void 방을_구독하지_않은_세션의_연결_종료는_무시한다() {
        // when
        registry.disconnected("등록된적-없는-세션");

        // then
        verify(gameRoomService, never()).leaveRoom(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 같은_세션이_같은_방을_다시_구독해도_중복_등록되지_않는다() {
        // given
        RoomMember member = new RoomMember(ROOM_ID, NICKNAME);
        registry.connected("session-1", member);

        // when: 재연결 없이 구독만 반복 (다중 구독)
        registry.connected("session-1", member);

        // then: 세션 하나가 끊기면 연결 없음으로 떨어져야 한다
        registry.disconnected("session-1");
        assertThat(registry.isConnected(member)).isFalse();
    }
}
