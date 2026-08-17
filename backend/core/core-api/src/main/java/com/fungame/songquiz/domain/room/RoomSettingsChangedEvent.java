package com.fungame.songquiz.domain.room;

public record RoomSettingsChangedEvent(Long roomId, RoomStateInfo state) {
}
