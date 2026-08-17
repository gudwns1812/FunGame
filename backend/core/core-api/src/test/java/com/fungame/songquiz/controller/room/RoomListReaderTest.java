package com.fungame.songquiz.controller.room;

import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.RoomInfo;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.enums.GameRoomStatus;
import com.fungame.songquiz.enums.GameType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.domain.room.RoomSettings;
import com.fungame.songquiz.domain.room.GamePlayer;

class RoomListReaderTest {

    private static final Long ROOM_ID = 9L;

    private final GameRoomService gameRoomService = mock(GameRoomService.class);
    private final RoomPresence roomPresence = new RoomPresence();
    private final RoomListReader roomListReader = new RoomListReader(gameRoomService, roomPresence);

    @Test
    @DisplayName("방 목록의 인원수는 저장된 참가자가 아니라 실제 접속자 수다.")
    void currentPlayersCountsConnectedSessions() {
        given(gameRoomService.findAllRooms()).willReturn(List.of(roomWithStoredPlayers(3)));
        roomPresence.arrive("session-1", new RoomMember(ROOM_ID, 1L, "접속자"));

        assertThat(roomListReader.findAllRooms())
                .extracting(RoomInfo::currentPlayers)
                .containsExactly(1);
    }

    @Test
    @DisplayName("아무도 접속해 있지 않으면 인원수는 0이다.")
    void currentPlayersIsZeroWithoutConnection() {
        given(gameRoomService.findAllRooms()).willReturn(List.of(roomWithStoredPlayers(3)));

        assertThat(roomListReader.findAllRooms())
                .extracting(RoomInfo::currentPlayers)
                .containsExactly(0);
    }

    private static RoomInfo roomWithStoredPlayers(int storedPlayers) {
        return new RoomInfo(ROOM_ID,
                new RoomSettings(GameType.SONG, "방 제목", 8, Category.KPOP, 10, 0, CSQuizDifficulty.HARD),
                GamePlayer.createNewPlayer(1L, "방장"), GameRoomStatus.WAITING, storedPlayers);
    }
}
