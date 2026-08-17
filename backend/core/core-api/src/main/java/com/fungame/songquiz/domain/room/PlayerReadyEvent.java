package com.fungame.songquiz.domain.room;

public record PlayerReadyEvent(Long roomId, GamePlayer player, boolean isAllReady, RoomStateInfo state) {
}
