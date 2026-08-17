package com.fungame.songquiz.domain.room;

public record PlayerJoinEvent(Long roomId, GamePlayer player, RoomStateInfo state) {
}
