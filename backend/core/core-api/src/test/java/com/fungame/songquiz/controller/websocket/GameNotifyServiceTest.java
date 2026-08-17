package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.controller.response.ApiResponse;
import com.fungame.songquiz.controller.response.RoomStateResponse;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.domain.room.PlayerJoinEvent;
import com.fungame.songquiz.domain.room.PlayerLeaveEvent;
import com.fungame.songquiz.domain.room.RoomSettings;
import com.fungame.songquiz.domain.room.RoomSettingsChangedEvent;
import com.fungame.songquiz.domain.room.RoomStateInfo;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.enums.GameRoomStatus;
import com.fungame.songquiz.enums.GameType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("방 이벤트는 델타가 아니라 바뀐 뒤의 방 전체를 싣는다")
class GameNotifyServiceTest {

    private static final Long ROOM_ID = 7L;
    private static final GamePlayer HOST = new GamePlayer(1L, "방장", true);
    private static final GamePlayer GUEST = new GamePlayer(2L, "참가자", false);

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final GameNotifyService gameNotifyService = new GameNotifyService(messagingTemplate);

    @Test
    @DisplayName("입장 이벤트에 참가자 전체와 version 이 실린다.")
    void playerJoinCarriesWholeRoom() {
        gameNotifyService.handlePlayerJoin(new PlayerJoinEvent(ROOM_ID, GUEST, state(3)));

        Map<String, Object> payload = capturedPayload();
        assertThat(payload).containsEntry("type", "PLAYER_JOIN")
                .containsEntry("memberId", GUEST.memberId())
                .containsEntry("nickname", GUEST.nickname());
        assertThat(payload.get("room")).isEqualTo(RoomStateResponse.from(state(3)));
    }

    @Test
    @DisplayName("퇴장 이벤트도 같은 모양으로 방 전체를 싣는다.")
    void playerLeaveCarriesWholeRoom() {
        gameNotifyService.handlePlayerLeave(new PlayerLeaveEvent(ROOM_ID, GUEST, state(4)));

        Map<String, Object> payload = capturedPayload();
        assertThat(payload).containsEntry("type", "PLAYER_LEAVE");
        assertThat(payload.get("room")).isEqualTo(RoomStateResponse.from(state(4)));
    }

    @Test
    @DisplayName("설정 변경 이벤트는 설정과 방 전체를 함께 싣는다. 준비 상태가 초기화되기 때문이다.")
    void settingsChangeCarriesSettingsAndRoom() {
        gameNotifyService.handleRoomSettingsChanged(new RoomSettingsChangedEvent(ROOM_ID, state(5)));

        Map<String, Object> payload = capturedPayload();
        assertThat(payload).containsEntry("type", "ROOM_SETTINGS_CHANGED").containsKey("settings");
        assertThat(payload.get("room")).isEqualTo(RoomStateResponse.from(state(5)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedPayload() {
        ArgumentCaptor<ApiResponse<Object>> captor = ArgumentCaptor.forClass(ApiResponse.class);
        verify(messagingTemplate).convertAndSend(eq(StompDestination.room(ROOM_ID)), captor.capture());

        return (Map<String, Object>) captor.getValue().getData();
    }

    private static RoomStateInfo state(long version) {
        return new RoomStateInfo(ROOM_ID, version, GameRoomStatus.WAITING,
                new RoomSettings(GameType.SONG, "방 제목", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD),
                List.of(HOST, GUEST), HOST);
    }
}
