package com.fungame.songquiz.domain.room;

public record PlayerKickedEvent(Long roomId, GamePlayer player, RoomStateInfo state) {
}
