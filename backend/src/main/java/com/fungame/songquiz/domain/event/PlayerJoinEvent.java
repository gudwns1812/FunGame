package com.fungame.songquiz.domain.event;

public record PlayerJoinEvent(Long roomId, String playerName) {
}
