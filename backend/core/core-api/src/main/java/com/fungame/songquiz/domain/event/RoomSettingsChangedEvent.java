package com.fungame.songquiz.domain.event;

import com.fungame.songquiz.domain.dto.RoomSettingsInfo;

public record RoomSettingsChangedEvent(Long roomId, RoomSettingsInfo settings) {
}
