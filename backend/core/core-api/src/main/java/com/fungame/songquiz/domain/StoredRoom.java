package com.fungame.songquiz.domain;

import com.fungame.songquiz.enums.GameRoomStatus;
import java.time.Instant;
import java.util.List;

public record StoredRoom(
        Long roomId,
        RoomSettings settings,
        GameRoomStatus status,
        Long hostId,
        List<GamePlayer> players,
        Instant lastActivityTime
) {

    public String hostNickname() {
        return players.stream()
                .filter(player -> player.memberId().equals(hostId))
                .map(GamePlayer::nickname)
                .findFirst()
                .orElse(null);
    }
}
