package com.fungame.songquiz.controller.room;

import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.RoomInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoomListReader {

    private final GameRoomService gameRoomService;
    private final RoomPresence roomPresence;

    public List<RoomInfo> findAllRooms() {
        return gameRoomService.findAllRooms().stream()
                .map(room -> room.withConnectedPlayers(roomPresence.countConnectedIn(room.roomId())))
                .toList();
    }
}
