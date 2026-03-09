package com.fungame.songquiz.domain.event;

public record PlayerLeaveEvent(Long roomId, String playerName) {
}
