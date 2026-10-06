package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.enums.GameRoomStatus;
import java.time.Instant;
import java.util.List;

public record RoomSnapshot(
        Long roomId,
        RoomSettings settings,
        List<GamePlayer> players,
        Long hostId,
        GameRoomStatus status,
        Instant lastActivityTime,
        long version
) {
}
