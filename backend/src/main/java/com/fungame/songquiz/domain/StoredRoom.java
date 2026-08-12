package com.fungame.songquiz.domain;

import java.time.Instant;
import java.util.List;

public record StoredRoom(
        Long roomId,
        RoomSettings settings,
        GameRoomStatus status,
        String host,
        List<GamePlayer> players,
        Instant lastActivityTime
) {
}
